package tom.assistant.service.agent.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentResponseDeserializer extends JsonDeserializer<AgentResponse<?>> {

	private final AgentResponseTypeRegistry registry;

	public AgentResponseDeserializer(AgentResponseTypeRegistry registry) {
		this.registry = registry;
	}

	@Override
	public AgentResponse<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		JsonNode root = mapper.readTree(p);

		AgentAction action = AgentAction.valueOf(root.get("action").asText());
		JsonNode payloadNode = root.get("payload");

		Class<?> payloadClass = registry.get(action);

		Object payload = mapper.treeToValue(payloadNode, payloadClass);

		AgentResponse<Object> request = new AgentResponse<>();
		request.setAction(action);
		request.setPayload(payload);

		return request;
	}
}