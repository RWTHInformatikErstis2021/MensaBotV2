package de.doetsch.mensabot.canteen.deserializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.doetsch.mensabot.canteen.Canteen;

import java.io.IOException;

public class CanteenDeserializer extends StdDeserializer<Canteen> {
	protected CanteenDeserializer(Class<?> vc){
		super(vc);
	}
	protected CanteenDeserializer(JavaType valueType){
		super(valueType);
	}
	protected CanteenDeserializer(StdDeserializer<?> src){
		super(src);
	}
	
	@Override
	public Canteen deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException{
		JsonNode node = p.getCodec().readTree(p);
		Canteen.Coordinates coordinates = null;
		if(node.has("coordinates") && !node.get("coordinates").isNull()){
			ArrayNode arrayNode = (ArrayNode)node.get("coordinates");
			coordinates = new Canteen.Coordinates(arrayNode.get(0).doubleValue(), arrayNode.get(1).doubleValue());
		}
		return new Canteen(node.get("id").asInt(), node.get("name").asText(), node.get("city").asText(), node.get("address").asText(), coordinates);
	}
}
