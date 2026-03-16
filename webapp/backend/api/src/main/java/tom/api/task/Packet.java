package tom.api.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Packet {

	private static ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT)
			.addModule(new JavaTimeModule()).build();

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

		JsonNode node = resolveNode(path);

		if (node == null || node.isNull()) {
			return null;
		}

		if (node.isValueNode()) {
			return node.asText();
		}

		return node.toString();
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

	private JsonNode resolveNode(String path) {
		JsonNode current = toJsonTree();
		List<Object> tokens = tokenizePath(path);

		for (Object token : tokens) {
			if (current == null) {
				return null;
			}

			if (token instanceof String) {
				current = current.get((String) token);
			} else if (token instanceof Integer) {
				int index = (Integer) token;
				if (!current.isArray() || index >= current.size()) {
					return null;
				}
				current = current.get(index);
			}
		}

		return current;
	}

	private static List<Object> tokenizePath(String path) {
		List<Object> tokens = new ArrayList<>();
		int i = 0;

		while (i < path.length()) {
			char c = path.charAt(i);

			if (c == '.') {
				i++;
				continue;
			}

			if (c == '[') {
				int end = path.indexOf(']', i);
				String inside = path.substring(i + 1, end).trim();

				if (inside.matches("\\d+")) {
					tokens.add(Integer.parseInt(inside));
				} else {
					inside = inside.replaceAll("^['\"]|['\"]$", "");
					tokens.add(inside);
				}

				i = end + 1;
				continue;
			}

			int start = i;
			while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[') {
				i++;
			}

			tokens.add(path.substring(start, i));
		}

		return tokens;
	}

}