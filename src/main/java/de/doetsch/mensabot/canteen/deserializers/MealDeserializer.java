package de.doetsch.mensabot.canteen.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import de.doetsch.mensabot.canteen.Meal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class MealDeserializer extends StdDeserializer<Meal> {
	private static final Logger logger = LogManager.getLogger(MealDeserializer.class);
	
	public MealDeserializer(){
		super((Class<?>)null);
	}
	
	@Override
	public Meal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);
		List<String> notes = new ArrayList<>();
		for(JsonNode n : node.get("notes")) notes.add(n.asText());
		Map<String, Double> prices = new HashMap<>();
		for(Iterator<Map.Entry<String, JsonNode>> it = node.get("prices").fields(); it.hasNext(); ){
			Map.Entry<String, JsonNode> entry = it.next();
			prices.put(entry.getKey(), entry.getValue().asDouble());
		}
		return new Meal(null, node.get("id").asInt(), node.get("name").asText(), notes, node.get("category").asText(), prices);
	}
}
