package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.doetsch.mensabot.canteen.deserializers.MealDeserializer;
import de.doetsch.mensabot.data.types.RatingRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(using = MealDeserializer.class)
public record Meal(int id, String name, List<String> notes, String category, Map<String, Double> prices) {
	private static final Logger logger = LogManager.getLogger(Meal.class);
	
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
		return RatingRepository.findByMeal(name).flatMap(ratings -> {
			if(ratings.isEmpty()) return Mono.empty();
			return Mono.just(new Rating(ratings));
		});
	}
	
	public static class Rating {
		private final double rating;
		private final int ratingCount;
		public Rating(List<de.doetsch.mensabot.data.types.Rating> ratings){
			ratingCount = ratings.size();
			rating = ratings.stream().mapToInt(de.doetsch.mensabot.data.types.Rating::getRating).average().orElse(0d);
		}
		public double getRating(){return rating;}
		public int getRatingCount(){return ratingCount;}
	}
}
