package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.doetsch.mensabot.canteen.deserializers.MealDeserializer;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(using = MealDeserializer.class)
public record Meal(int id, String name, List<String> notes, String category, Map<String, Double> prices) {
	public double getStudentPrice(){
		Double price = prices.get("students");
		if(price != null) return price;
		return prices.values().stream().filter(Objects::nonNull).findFirst().orElse(0d);
	}
	public double getOthersPrice(){
		Double price = prices.get("others");
		if(price != null) return price;
		return 0d;
	}
	
	public Mono<Rating> getRating(){
		// TODO implement
		return Mono.empty();
	}
	
	public static class Rating {
		private final double rating;
		private final int ratingCount;
		public Rating(List<Integer> ratings){
			ratingCount = ratings.size();
			rating = ratings.stream().mapToInt(x->x).average().orElse(0d);
		}
		public double getRating(){return rating;}
		public int getRatingCount(){return ratingCount;}
	}
}
