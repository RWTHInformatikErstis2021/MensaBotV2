package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.doetsch.mensabot.canteen.deserializers.CanteenDeserializer;
import de.doetsch.mensabot.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonDeserialize(using=CanteenDeserializer.class)
public final class Canteen {
	
	private static final Logger logger = LogManager.getLogger(Canteen.class);
	
	private final int id;
	private final String name;
	private final String city;
	private final String address;
	private final Coordinates coordinates;
	
	public Canteen(int id, String name, String city, String address, Coordinates coordinates){
		this.id = id;
		this.name = name;
		this.city = city;
		this.address = address;
		this.coordinates = coordinates;
		this.mealsMono = CanteenAPI.getMeals(id)
				.flatMap(mealsMap -> {
					Instant now = Instant.now();
					return Flux.range(1, 7)
							.flatMap(days -> {
								String date = Util.formatDate(now.minus(days, ChronoUnit.DAYS));
								return CanteenAPI.getMeals(id, date).map(meals -> Tuples.of(date, meals));
							})
							.collectMap(Tuple2::getT1, Tuple2::getT2)
							.map(mealsMap2 -> {
								Map<String, List<Meal>> merged = new HashMap<>(mealsMap);
								merged.putAll(mealsMap2);
								return merged;
							});
				})
				.cache(Duration.of(30, ChronoUnit.MINUTES));
	}
	
	public record Coordinates(double x, double y) {
	}
	
	private final Mono<Map<String, List<Meal>>> mealsMono;
	public Mono<Map<String, List<Meal>>> getMeals(){
		return mealsMono;
	}
	public Mono<List<Meal>> getMeals(Instant instant){
		String date = Util.formatDate(instant);
		return mealsMono.flatMapIterable(Map::entrySet)
				.filter(entry -> entry.getKey().equals(date))
				.next()
				.map(Map.Entry::getValue)
				.switchIfEmpty(CanteenAPI.getMeals(id, date)); // TODO cache
	}
	
	public int getId(){return id;}
	public String getName(){return name;}
	public String getCity(){return city;}
	public String getAddress(){return address;}
	@Nullable public Coordinates getCoordinates(){return coordinates;}
	
	@Override
	public boolean equals(Object obj){
		if(obj == this) return true;
		if(obj == null || obj.getClass() != this.getClass()) return false;
		var that = (Canteen)obj;
		return this.id == that.id &&
				Objects.equals(this.name, that.name) &&
				Objects.equals(this.city, that.city) &&
				Objects.equals(this.address, that.address) &&
				Objects.equals(this.coordinates, that.coordinates);
	}
	
	@Override
	public int hashCode(){
		return Objects.hash(id, name, city, address, coordinates);
	}
	
	@Override
	public String toString(){
		return "Canteen[" +
				"id=" + id + ", " +
				"name=" + name + ", " +
				"city=" + city + ", " +
				"address=" + address + ", " +
				"coordinates=" + coordinates + ']';
	}
	
	public enum DefaultCanteen {
		ACADEMICA(187, "Academica"),
		BISTRO(94, "Bistro"),
		VITA(96, "Vita"),
		AHORN(95, "Ahornstraße"),
		BAYERNALLEE(97, "Bayernallee"),
		JUELICH(100, "Jülich")
		;
		private final int id;
		private final String displayName;
		DefaultCanteen(int id, String displayName){
			this.id = id;
			this.displayName = displayName;
		}
		public int getId(){return id;}
		public String getDisplayName(){return displayName;}
	}
	
}
