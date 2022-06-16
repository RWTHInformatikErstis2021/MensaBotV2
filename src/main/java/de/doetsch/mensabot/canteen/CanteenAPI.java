package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.netty.http.client.HttpClient;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CanteenAPI {
	
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
	
	private static Mono<Tuple2<List<Canteen>, Integer>> requestCanteenPage(int page){
		return client.get().uri("/canteens/?page=" + page).responseSingle((response, rawBody) -> rawBody.asString().map(content -> {
			int nextPage = -1;
			try{
				int totalPages = Integer.parseInt(response.responseHeaders().get("X-Total-Pages"));
				if(page < totalPages) nextPage = page + 1;
			}catch(NumberFormatException ex){
				// TODO log
			}
			ObjectMapper mapper = new ObjectMapper();
			try{
				ArrayNode canteensNode = (ArrayNode)mapper.readTree(content);
				// TODO make canteens deserializable
				List<Canteen> canteens = mapper.convertValue(canteensNode, List.class);
				return Tuples.of(canteens, nextPage);
			}catch(JsonProcessingException ex){
				// TODO log
				return Tuples.of(new ArrayList<>(), nextPage);
			}
		}));
	}
	
}
