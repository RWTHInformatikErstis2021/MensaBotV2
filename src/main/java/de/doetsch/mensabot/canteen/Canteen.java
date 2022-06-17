package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.doetsch.mensabot.canteen.deserializers.CanteenDeserializer;
import de.doetsch.mensabot.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@JsonDeserialize(using=CanteenDeserializer.class)
public final class Canteen {
	
	private static final Logger logger = LogManager.getLogger(Canteen.class);
	private final int id;
	private final String name;
	private final String city;
	private final String address;
	private final Coordinates coordinates;
	
	public Canteen(int id, String name, String city, String address,
								 Coordinates coordinates){
		this.id = id;
		this.name = name;
		this.city = city;
		this.address = address;
		this.coordinates = coordinates;
	}
	
	public record Coordinates(double x, double y) {
	}
	
	private final Cache<String, Mono<List<Meal>>> mealCache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.of(24, ChronoUnit.HOURS))
			.build();
	public Mono<List<Meal>> getMeals(Instant instant){
		String date = Util.formatDate(instant);
		return mealCache.get(date, s -> CanteenAPI.getMeals(id, s).cache(Duration.of(30, ChronoUnit.MINUTES)));
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
		ACADEMICA(187),
		BISTRO(94),
		VITA(96),
		AHORN(95),
		BAYERNALLEE(97),
		JUELICH(100)
		;
		private final int id;
		private DefaultCanteen(int id){
			this.id = id;
		}
		public int getId(){return id;}
	}
	
}
