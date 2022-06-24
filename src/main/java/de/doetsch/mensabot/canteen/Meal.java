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

@JsonDeserialize(using=MealDeserializer.class)
public final class Meal {
	private static final Logger logger = LogManager.getLogger(Meal.class);
	private final Canteen canteen;
	private final int id;
	private final String name;
	private final List<String> notes;
	private final String category;
	private final Map<String, Double> prices;
	
	public Meal(Canteen canteen, int id, String name, List<String> notes, String category, Map<String, Double> prices){
		this.canteen = canteen;
		this.id = id;
		this.name = name;
		this.notes = notes;
		this.category = category;
		this.prices = prices;
	}
	
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
		return RatingRepository.findByMeal(canteen.getCity(), name).flatMap(ratings -> {
			if(ratings.isEmpty()) return Mono.empty();
			return Mono.just(new Rating(ratings));
		});
	}
	
	public Meal withCanteen(Canteen canteen){
		return new Meal(canteen, id, name, notes, category, prices);
	}
	
	public Canteen getCanteen(){return canteen;}
	public int getId(){return id;}
	public String getName(){return name;}
	public List<String> getNotes(){return notes;}
	public String getCategory(){return category;}
	public Map<String, Double> getPrices(){return prices;}
	
	@Override
	public boolean equals(Object obj){
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Meal)obj;
		return Objects.equals(this.canteen, that.canteen) &&
				this.id == that.id &&
				Objects.equals(this.name, that.name) &&
				Objects.equals(this.notes, that.notes) &&
				Objects.equals(this.category, that.category) &&
				Objects.equals(this.prices, that.prices);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(canteen, id, name, notes, category, prices);
	}
	
	@Override
	public String toString(){
		return "Meal[" +
				"canteen=" + canteen + ", " +
				"id=" + id + ", " +
				"name=" + name + ", " +
				"notes=" + notes + ", " +
				"category=" + category + ", " +
				"prices=" + prices + ']';
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
