package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CanteenAPI {
	
	private static final Logger logger = LogManager.getLogger(CanteenAPI.class);
	
	private static final HttpClient client = HttpClient.create().baseUrl("https://openmensa.org/api/v2");
	
	private static final Mono<List<Canteen>> canteensMono = requestCanteenPage(1).expand(TupleUtils.function((canteens, nextPage) -> {
		if(nextPage < 0) return Mono.empty();
		else return requestCanteenPage(nextPage);
	})).map(Tuple2::getT1).collectList().map(lists -> {
		List<Canteen> list = new ArrayList<>();
		for(List<Canteen> l : lists) list.addAll(l);
		return list;
	}).cache(Duration.ofHours(24));
	public static Mono<List<Canteen>> getCanteens(){
		return canteensMono;
	}
	public static Mono<Canteen> getCanteen(int id){
		return getCanteens().flatMap(l -> Mono.justOrEmpty(l.stream().filter(m -> m.getId() == id).findAny()));
	}
	
	private static Mono<Tuple2<List<Canteen>, Integer>> requestCanteenPage(int page){
		return client.get().uri("/canteens/?page=" + page).responseSingle((response, rawBody) -> rawBody.asString(StandardCharsets.UTF_8).map(content -> {
			int nextPage = -1;
			try{
				int totalPages = Integer.parseInt(response.responseHeaders().get("X-Total-Pages"));
				if(page < totalPages) nextPage = page + 1;
			}catch(NumberFormatException ex){
				// TODO log
			}
			try{
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode canteensNode = (ArrayNode)mapper.readTree(content);
				List<Canteen> canteens = List.of(mapper.convertValue(canteensNode, Canteen[].class));
				return Tuples.of(canteens, nextPage);
			}catch(JsonProcessingException ex){
				// TODO log
				return Tuples.of(new ArrayList<>(), nextPage);
			}
		}));
	}
	
	public static Mono<Map<String, List<Meal>>> getMeals(int mensaId){
		return client.get().uri("/canteens/" + mensaId + "/meals").responseSingle((response, rawBody) -> rawBody.asString(StandardCharsets.UTF_8).flatMap(content -> {;
			Map<String, List<Meal>> days = new HashMap<>();
			try{
				ObjectMapper mapper = new ObjectMapper();
				for(JsonNode dayNode : mapper.readTree(content)){
					List<Meal> rawMeals = List.of(mapper.convertValue(dayNode.get("meals"), Meal[].class));
					List<Meal> meals = new ArrayList<>();
					for(Meal meal : rawMeals){
						if(meal.category().equalsIgnoreCase("hauptbeilagen") || meal.category().equalsIgnoreCase("nebenbeilage")){
							if(meal.name().strip().endsWith(" oder")){
								String newName = meal.name().strip();
								newName = newName.substring(0, newName.length() - 5);
								meals.add(new Meal(meal.id(), newName, meal.notes(), meal.category(), meal.prices()));
							}else if(meal.name().contains(" oder ")){
								String[] mealNames = meal.name().split(" oder ");
								for(String mealName : mealNames){
									meals.add(new Meal(meal.id(), mealName.strip(), meal.notes(), meal.category(), meal.prices()));
								}
							}else{
								meals.add(meal);
							}
						}else{
							meals.add(meal);
						}
					}
					days.put(dayNode.get("date").asText(), meals);
				}
			}catch(JsonProcessingException e){
				// TODO log
			}
			return Mono.just(days);
		}));
	}
	
}
