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
	
	private static final Mono<Map<Integer, Canteen>> canteensMono = requestCanteenPage(1).expand(TupleUtils.function((canteens, nextPage) -> {
		if(nextPage < 0) return Mono.empty();
		else return requestCanteenPage(nextPage);
	})).map(Tuple2::getT1).collectList().map(lists -> {
		Map<Integer, Canteen> map = new HashMap<>();
		for(List<Canteen> l : lists) for(Canteen c : l) map.put(c.getId(), c);
		return map;
	}).cache(Duration.ofHours(24));
	public static Mono<Map<Integer, Canteen>> getCanteens(){
		return canteensMono;
	}
	public static Mono<Canteen> getCanteen(int id){
		return getCanteens().flatMap(map -> Mono.justOrEmpty(map.get(id)));
	}
	
	private static Mono<Tuple2<List<Canteen>, Integer>> requestCanteenPage(int page){
		return client.get().uri("/canteens/?page=" + page).responseSingle((response, rawBody) -> rawBody.asString(StandardCharsets.UTF_8).map(content -> {
			if(response.status().code() < 200 || response.status().code() >= 300){
				logger.warn("Canteens request responded with non 2XX status code {} with reason {}", response.status().code(), response.status().reasonPhrase());
			}
			int nextPage = -1;
			try{
				int totalPages = Integer.parseInt(response.responseHeaders().get("X-Total-Pages"));
				if(page < totalPages) nextPage = page + 1;
			}catch(NumberFormatException ex){
				logger.error("Error while parsing X-Total-Pages in canteen request", ex);
			}
			try{
				ObjectMapper mapper = new ObjectMapper();
				ArrayNode canteensNode = (ArrayNode)mapper.readTree(content);
				List<Canteen> canteens = List.of(mapper.convertValue(canteensNode, Canteen[].class));
				return Tuples.of(canteens, nextPage);
			}catch(JsonProcessingException ex){
				logger.error("Error while trying to parse canteen request response", ex);
				return Tuples.of(new ArrayList<>(), nextPage);
			}
		}));
	}
	
	public static Mono<Map<String, List<Meal>>> getMeals(int canteenId){
		return getCanteen(canteenId).flatMap(canteen -> client.get().uri("/canteens/" + canteenId + "/meals").responseSingle((response, rawBody) -> rawBody.asString(StandardCharsets.UTF_8).flatMap(content -> {
			if(response.status().code() < 200 || response.status().code() >= 300){
				logger.warn("Meals request responded with non 2XX status code {} with reason {}", response.status().code(), response.status().reasonPhrase());
			}
			Map<String, List<Meal>> days = new HashMap<>();
			try{
				ObjectMapper mapper = new ObjectMapper();
				for(JsonNode dayNode : mapper.readTree(content)){
					List<Meal> rawMeals = List.of(mapper.convertValue(dayNode.get("meals"), Meal[].class));
					days.put(dayNode.get("date").asText(), processMeals(canteen, rawMeals));
				}
			}catch(JsonProcessingException ex){
				logger.error("Error while trying to parse meals", ex);
			}
			return Mono.just(days);
		})));
	}
	
	public static Mono<List<Meal>> getMeals(int canteenId, String date){
		return getCanteen(canteenId).flatMap(canteen -> client.get().uri("/canteens/" + canteenId + "/days/" + date + "/meals").responseSingle((response, rawBody) -> rawBody.asString(StandardCharsets.UTF_8).flatMap(content -> {
			if(response.status().code() == 404) return Mono.empty();
			if(response.status().code() < 200 || response.status().code() >= 300){
				logger.warn("Meals request responded with non 2XX status code {} with reason {}", response.status().code(), response.status().reasonPhrase());
			}
			ObjectMapper mapper = new ObjectMapper();
			try{
				List<Meal> rawMeals = List.of(mapper.convertValue(mapper.readTree(content), Meal[].class));
				return Mono.just(processMeals(canteen, rawMeals));
			}catch(JsonProcessingException ex){
				logger.error("Error while trying to parse meals", ex);
				return Mono.just(new ArrayList<>());
			}
		})));
	}
	
	private static List<Meal> processMeals(Canteen canteen, List<Meal> rawMeals){
		List<Meal> meals = new ArrayList<>();
		for(Meal meal : rawMeals){
			if(meal.getCategory().equalsIgnoreCase("hauptbeilagen") || meal.getCategory().equalsIgnoreCase("nebenbeilage")){
				if(meal.getName().strip().endsWith(" oder")){
					String newName = meal.getName().strip();
					newName = newName.substring(0, newName.length() - 5);
					meals.add(new Meal(canteen, meal.getId(), newName, meal.getNotes(), meal.getCategory(), meal.getPrices()));
				}else if(meal.getName().contains(" oder ")){
					String[] mealNames = meal.getName().split(" oder ");
					for(String mealName : mealNames){
						meals.add(new Meal(canteen, meal.getId(), mealName.strip(), meal.getNotes(), meal.getCategory(), meal.getPrices()));
					}
				}else{
					meals.add(meal.withCanteen(canteen));
				}
			}else{
				meals.add(meal.withCanteen(canteen));
			}
		}
		return meals;
	}
	
}
