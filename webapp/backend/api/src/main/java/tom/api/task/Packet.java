package tom.api.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Packet {

	private static ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT)
			.addModule(new JavaTimeModule()).build();

	private static final Pattern LIST_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\[(\\d+)]");

	private String id;
	private List<String> text;
	private List<Map<String, Object>> data;

	private transient JsonNode jsonCache;

	public Packet() {
		id = "";
		text = new ArrayList<>();
		data = new ArrayList<>();
	}

	public Packet(Packet other) {
		try {
			String json = other.toJson();
			Packet clone = mapper.readValue(json, Packet.class);

			this.id = clone.id;
			this.text = clone.text;
			this.data = clone.data;

		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to clone Packet", e);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
		jsonCache = null;
	}

	public void setIdFromPath(String path) {
		String value = resolve(path);
		if (value != null) {
			setId(value);
		}
	}

	public List<String> getText() {
		return text;
	}

	public void setText(List<String> text) {
		this.text = text != null ? text : new ArrayList<>();
		jsonCache = null;
	}

	public void addText(String text) {
		this.text.add(text);
	}

	public List<Map<String, Object>> getData() {
		return data;
	}

	public void setData(List<Map<String, Object>> data) {
		this.data = data != null ? data : new ArrayList<>();
		jsonCache = null;
	}

	public void addData(Map<String, Object> data) {
		this.data.add(data);
		jsonCache = null;
	}

	public void addDataList(List<Map<String, Object>> dataList) {
		data.addAll(dataList);
		jsonCache = null;
	}

	public void addTextList(List<String> textList) {
		this.text.addAll(textList);
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		if (!text.isEmpty()) {
			map.put("text", text);
		}
		if (!data.isEmpty()) {
			map.put("data", data);
		}
		return map;
	}

	public String toJson() throws JsonProcessingException {
		return mapper.writeValueAsString(this);
	}

	@Override
	public String toString() {
		String dataStr;
		try {
			if (data == null) {
				dataStr = "<<null>>";
			} else {
				dataStr = mapper.writeValueAsString(data);
			}
		} catch (JsonProcessingException e) {
			dataStr = data.toString();
		}
		return "Packet [id=" + id + ", text=" + text + ", data=" + dataStr + "]";
	}

	private JsonNode toJsonTree() {

		if (jsonCache == null) {
			jsonCache = mapper.valueToTree(this.toMap());
		}

		return jsonCache;
	}

	public String resolve(String path) {
		path = path.strip();

		if ("id".equalsIgnoreCase(path)) {
			return id;
		}

		if ("text".equalsIgnoreCase(path)) {
			return String.join(System.lineSeparator(), text);
		}

		JsonNode node = resolveNode(toJsonTree(), path);

		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}

		if (node.isValueNode()) {
			return node.asText();
		}

		return node.toString();
	}

	public JsonNode resolveNode(String path) {
		return resolveNode(toJsonTree(), path);
	}

	public List<JsonNode> resolveArray(String path) {
		JsonNode node = resolveNode(path);
		List<JsonNode> result = new ArrayList<>();
		if (node != null && node.isArray()) {
			node.forEach(result::add);
		}
		return result;
	}

	public boolean resolveBoolean(String path) {
		String value = resolve(path);
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	public Object resolveObject(String path) {
		JsonNode node = resolveNode(path);

		if (node == null) {
			return null;
		}

		if (node.isNumber()) {
			return node.numberValue();
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		if (node.isTextual()) {
			return node.textValue();
		}

		return node;
	}

	private static JsonNode resolveNode(JsonNode current, String path) {
		String[] parts = path.split("\\.", 2);
		String head = parts[0];
		String tail = parts.length > 1 ? parts[1] : null;

		JsonNode next;

		Matcher listMatcher = LIST_ACCESS_PATTERN.matcher(head);

		if (listMatcher.matches()) {
			String field = listMatcher.group(1);
			int index = Integer.parseInt(listMatcher.group(2));

			JsonNode arrayNode = current.get(field);

			if (arrayNode == null || !arrayNode.isArray() || index >= arrayNode.size()) {
				return null;
			}

			next = arrayNode.get(index);

		} else {
			next = current.get(head);

			if (next == null) {
				return null;
			}
		}

		if (tail == null) {
			return next;
		}

		return resolveNode(next, tail);
	}
}