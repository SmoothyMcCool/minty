package tom.assistant.service.agent.response;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tom.assistant.service.agent.model.AgentResponseType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmResponse {

	private static ObjectMapper Mapper = new ObjectMapper();

	private LlmStatus status;
	private String message;

	private AgentResponseType responseType;

	private JsonNode data;
	private String rawText;
	private JsonNode meta;

	@JsonAnySetter
	private Map<String, JsonNode> extra = new HashMap<>();

	@Override
	public String toString() {
		try {
			return Mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return "LlmResponse [status=" + status + ", message=" + message + ", responseType=" + responseType
					+ ", data=" + data + ", rawText=" + rawText + ", meta=" + meta + ", extra=" + extra + "]";
		}
	}

	public LlmStatus getStatus() {
		return status;
	}

	public void setStatus(LlmStatus status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public AgentResponseType getResponseType() {
		return responseType;
	}

	public void setResponseType(AgentResponseType responseType) {
		this.responseType = responseType;
	}

	public JsonNode getData() {
		return data;
	}

	public void setData(JsonNode data) {
		this.data = data;
	}

	public String getRawText() {
		return rawText;
	}

	public void setRawText(String rawText) {
		this.rawText = rawText;
	}

	public JsonNode getMeta() {
		return meta;
	}

	public void setMeta(JsonNode meta) {
		this.meta = meta;
	}

	public Map<String, JsonNode> getExtra() {
		return extra;
	}

	public static LlmResponse parse(String raw, AgentResponseType responseType)
			throws JsonMappingException, JsonProcessingException {

		if (responseType == AgentResponseType.Structured) {
			JsonNode node = Mapper.readTree(raw);
			ObjectNode normalized = Mapper.createObjectNode();

			normalized.put("status", safeStatus(node));
			normalized.put("message", safeMessage(node));

			JsonNode data = node.get("data");
			if (data == null || !data.isObject()) {
				ObjectNode reconstructed = Mapper.createObjectNode();

				for (Map.Entry<String, JsonNode> property : node.properties()) {
					String key = property.getKey();
					if (!key.equals("status") && !key.equals("message")) {
						reconstructed.set(key, property.getValue());
					}
				}

				normalized.set("data", reconstructed);
			} else {
				normalized.set("data", data);
			}

			LlmResponse response = Mapper.treeToValue(normalized, LlmResponse.class);
			response.setResponseType(AgentResponseType.Structured);
			if (response.getData() == null && !response.getExtra().isEmpty()) {
				ObjectNode objectNode = Mapper.createObjectNode();
				response.getExtra().forEach(objectNode::set);
				response.setData(objectNode);
			}
			return response;
		} else {
			LlmResponse response = new LlmResponse();
			response.setResponseType(AgentResponseType.RawText);
			response.setRawText(raw);
			return response;
		}

	}

	private static String safeStatus(JsonNode node) {
		if (node == null || !node.has("status")) {
			return LlmStatus.UNKNOWN.toString();
		}

		JsonNode statusNode = node.get("status");

		if (!statusNode.isTextual()) {
			return LlmStatus.UNKNOWN.toString();
		}

		String raw = statusNode.asText().trim();

		if (raw.isEmpty()) {
			return LlmStatus.UNKNOWN.toString();
		}

		try {
			return LlmStatus.valueOf(raw.toUpperCase()).toString();
		} catch (IllegalArgumentException ex) {
			return LlmStatus.UNKNOWN.toString();
		}
	}

	private static String safeMessage(JsonNode node) {
		if (node == null || !node.has("message")) {
			return "";
		}

		JsonNode messageNode = node.get("message");

		if (messageNode.isTextual()) {
			return messageNode.asText();
		}

		// fallback: convert anything else to string
		return messageNode.toString();
	}
}
