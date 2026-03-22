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
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

public class Packet {

	private static final ObjectMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT)
			.addModule(new JavaTimeModule()).build();

	private static final Configuration jaywayConfig = Configuration.builder()
			.jsonProvider(new JacksonJsonNodeJsonProvider(mapper)).mappingProvider(new JacksonMappingProvider(mapper))
			.options(Option.SUPPRESS_EXCEPTIONS).build();

	private static final ParseContext jayway = JsonPath.using(jaywayConfig);

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

	// -------------------------------------------------------------------------
	// Path resolution (via Jayway JSONPath)
	//
	// Paths may be supplied in two forms:
	// - Standard JSONPath: $.data[0].name (preferred, full Jayway syntax)
	// - Legacy shorthand: data[0].name ($ prefix added automatically)
	//
	// Special shorthands that bypass JSONPath entirely:
	// - "id" -> returns the packet id directly
	// - "text" -> returns all text entries joined by the system line separator
	// -------------------------------------------------------------------------

	public String resolve(String path) {
		path = path.strip();

		if ("id".equalsIgnoreCase(path)) {
			return id;
		}

		if ("text".equalsIgnoreCase(path)) {
			return String.join(System.lineSeparator(), text);
		}

		Object result = readPath(path);

		if (result == null) {
			return null;
		}

		if (result instanceof JsonNode node) {
			if (node.isNull())
				return null;
			if (node.isValueNode())
				return node.asText();
			return node.toString();
		}

		return result.toString();
	}

	public List<JsonNode> resolveArray(String path) {
		Object result = readPath(path);
		List<JsonNode> list = new ArrayList<>();

		if (result instanceof JsonNode node && node.isArray()) {
			node.forEach(list::add);
		}

		return list;
	}

	public JsonNode resolveNode(String path) {
		Object result = readPath(path);
		if (result instanceof JsonNode node) {
			return node;
		}
		return null;
	}

	public boolean resolveBoolean(String path) {
		String value = resolve(path);
		if (value == null) {
			return false;
		}
		return Boolean.parseBoolean(value);
	}

	public Object resolveObject(String path) {
		Object result = readPath(path);

		if (result instanceof JsonNode node) {
			if (node.isNumber())
				return node.numberValue();
			if (node.isBoolean())
				return node.booleanValue();
			if (node.isTextual())
				return node.textValue();
		}

		return result;
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	/**
	 * Evaluates a JSONPath expression against this packet's JSON tree. Accepts both
	 * full JSONPath ($.foo) and legacy shorthand (foo), in which case a "$." prefix
	 * is added automatically.
	 */
	private Object readPath(String path) {
		String normalized = path.startsWith("$") ? path : "$." + path;
		normalized = expandTopLevelShorthand(normalized);
		return jayway.parse(toJsonTree()).read(normalized);
	}

	/**
	 * If the path references $.data.x or $.text.x (without an explicit index),
	 * automatically inserts [0] so that $.data.x becomes $.data[0].x. Leaves paths
	 * that already have an index (e.g. $.data[1].x) untouched.
	 */
	private String expandTopLevelShorthand(String path) {
		if (path.matches("\\$\\.data(\\..+)") && !path.startsWith("$.data[")) {
			return path.replaceFirst("\\$\\.data", "\\$.data[0]");
		}
		if (path.matches("\\$\\.text(\\..+)") && !path.startsWith("$.text[")) {
			return path.replaceFirst("\\$\\.text", "\\$.text[0]");
		}
		return path;
	}

	private JsonNode toJsonTree() {
		if (jsonCache == null) {
			jsonCache = mapper.valueToTree(this.toMap());
		}
		return jsonCache;
	}
}