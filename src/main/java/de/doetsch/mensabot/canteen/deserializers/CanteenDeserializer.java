package de.doetsch.mensabot.canteen.deserializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.doetsch.mensabot.canteen.Canteen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class CanteenDeserializer extends StdDeserializer<Canteen> {
	private static final Logger logger = LogManager.getLogger(CanteenDeserializer.class);
	
	public CanteenDeserializer(){
		super((Class<?>)null);
	}
	
	@Override
	public Canteen deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		JsonNode node = p.getCodec().readTree(p);
		Canteen.Coordinates coordinates = null;
		if(node.has("coordinates") && !node.get("coordinates").isNull()){
			ArrayNode arrayNode = (ArrayNode)node.get("coordinates");
			coordinates = new Canteen.Coordinates(arrayNode.get(0).doubleValue(), arrayNode.get(1).doubleValue());
		}
		return new Canteen(node.get("id").asInt(), node.get("name").asText(), node.get("city").asText(), node.get("address").asText(), coordinates);
	}
}
