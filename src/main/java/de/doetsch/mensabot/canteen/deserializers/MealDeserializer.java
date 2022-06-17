package de.doetsch.mensabot.canteen.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.doetsch.mensabot.canteen.Meal;

import java.io.IOException;
import java.util.*;

public class MealDeserializer extends StdDeserializer<Meal> {
	protected MealDeserializer(Class<?> vc){
		super(vc);
	}
	protected MealDeserializer(JavaType valueType){
		super(valueType);
	}
	protected MealDeserializer(StdDeserializer<?> src){
		super(src);
	}
	
	@Override
	public Meal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException{
		JsonNode node = p.getCodec().readTree(p);
		List<String> notes = new ArrayList<>();
		for(JsonNode n : node.get("notes")) notes.add(n.asText());
		Map<String, Double> prices = new HashMap<>();
		for(Iterator<Map.Entry<String, JsonNode>> it = node.get("prices").fields(); it.hasNext(); ){
			Map.Entry<String, JsonNode> entry = it.next();
			prices.put(entry.getKey(), entry.getValue().asDouble());
		}
		return new Meal(node.get("id").asInt(), node.get("name").asText(), notes, node.get("category").asText(), prices);
	}
}
