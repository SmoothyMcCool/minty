package tom.tasks.transformer.textformatter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.MintyTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Format Text", configClass = "tom.tasks.transformer.textformatter.TextFormatterConfig")
public class TextFormatter implements MintyTask {

	private static final Logger logger = LogManager.getLogger(TextFormatter.class);

	private final UUID uuid = UUID.randomUUID();
	private TextFormatterConfig config;
	private Map<String, Object> input;
	private Map<String, Object> result = Map.of();

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");
	private static final Pattern LIST_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+)\\[(\\d+)]");

	public TextFormatter() {
	}

	public TextFormatter(TextFormatterConfig config) {
		this.config = config;
	}

	@Override
	public String taskName() {
		return "FormatText-" + uuid.toString();
	}

	@Override
	public Map<String, Object> getResult() {
		return result;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(config.getFormat());
		StringBuffer sb = new StringBuffer();

		// Make sure input.data is a Json object.
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root;
		try {
			root = mapper.readTree((String) input.get("Data"));
		} catch (Exception e) {
			logger.warn("Could not parse input into JSON object.");
			return List.of();
		}

		while (matcher.find()) {
			String placeholder = matcher.group(0); // e.g. "{user.name}"
			String path = matcher.group(1).trim(); // e.g. "user.name"

			JsonNode valueNode = resolvePath(root, path);

			if (valueNode != null && !valueNode.isMissingNode() && !valueNode.isNull()) {
				matcher.appendReplacement(sb, Matcher.quoteReplacement(valueNode.asText()));
			} else {
				// leave placeholder as-is
				matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
			}
		}
		matcher.appendTail(sb);

		result = Map.of("Data", sb.toString());
		return List.of(result);
	}

	@Override
	public void setInput(Map<String, Object> input) {
		this.input = input;
	}

	@Override
	public String expects() {
		return "This task produces text output based on the configuration provided. "
				+ "It scans for text in the form {path.to.json[1].element} and make appropriate "
				+ "substitutions from the input JSON object. The result is returned in \"Data\".";
	}

	@Override
	public String produces() {
		return "The the provided config template with substitutions made from the received input.";
	}

	private static JsonNode resolvePath(JsonNode current, String path) {

		String[] parts = path.split("\\.", 2); // head + remainder
		String head = parts[0];
		String tail = (parts.length > 1) ? parts[1] : null;

		JsonNode next = null;

		Matcher listMatcher = LIST_ACCESS_PATTERN.matcher(head);
		if (listMatcher.matches()) {
			// List access, e.g. friends[0]
			String field = listMatcher.group(1);
			int index = Integer.parseInt(listMatcher.group(2));

			JsonNode arrayNode = current.get(field);
			if (arrayNode != null && arrayNode.isArray() && index >= 0 && index < arrayNode.size()) {
				next = arrayNode.get(index);
			} else {
				return null;
			}

		} else {
			// Regular map access
			next = current.get(head);
			if (next == null) {
				return null;
			}
		}

		if (tail == null) {
			return next;
		}

		return resolvePath(next, tail);
	}
}
