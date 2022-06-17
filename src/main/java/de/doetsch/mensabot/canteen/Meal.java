package de.doetsch.mensabot.canteen;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.doetsch.mensabot.canteen.deserializers.MealDeserializer;

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
}
