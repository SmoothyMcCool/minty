package tom.tasks.transform.formattext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import tom.api.task.Packet;

public class TemplateRenderer {

	private static final Pattern LOOP_PATTERN = Pattern.compile("\\{#([^{}]+)}([\\s\\S]*?)\\{/\\1}");
	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)}");

	public String render(Packet packet, String template) {
		JsonNode root = packetToNode(packet);
		return renderBlock(packet, root, template).strip();
	}

	private String renderBlock(Packet packet, JsonNode context, String template) {
		String processed = processLoops(packet, context, template);
		return processPlaceholders(packet, context, processed);
	}

	private String processLoops(Packet packet, JsonNode context, String template) {
		Matcher matcher = LOOP_PATTERN.matcher(template);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String path = matcher.group(1).strip();
			String block = matcher.group(2);

			// normalize leading/trailing newline caused by template layout
			if (block.startsWith("\n")) {
				block = block.substring(1);
			}

			JsonNode node = resolveNode(packet, context, path);
			StringBuilder replacement = new StringBuilder();

			if (node != null && node.isArray()) {
				for (JsonNode element : node) {
					String rendered = renderBlock(packet, element, block);
					replacement.append(rendered);
				}
			}

			matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
		}

		matcher.appendTail(sb);
		return sb.toString();
	}

	private String processPlaceholders(Packet packet, JsonNode context, String template) {
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String placeholder = matcher.group(1).strip();
			JsonNode node = resolveNode(packet, context, placeholder);

			if (node != null && !node.isNull()) {
				String value;
				if (node.isValueNode()) {
					value = node.asText();
				} else {
					value = node.toString();
				}
				matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
			} else {
				// preserve placeholder
				matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
			}
		}

		matcher.appendTail(sb);
		return sb.toString();
	}

	private JsonNode resolveNode(Packet packet, JsonNode context, String path) {
		// try relative resolution first
		JsonNode node = resolveAgainstNode(context, path);

		if (node != null) {
			return node;
		}

		// fallback to full packet path
		Object obj = packet.resolveObject(path);

		if (obj instanceof JsonNode) {
			return (JsonNode) obj;
		}

		if (obj != null) {
			return packetToNode(packet).get(path);
		}

		return null;
	}

	private JsonNode resolveAgainstNode(JsonNode node, String path) {
		List<Object> tokens = tokenize(path);
		JsonNode current = node;

		for (Object token : tokens) {
			if (current == null) {
				return null;
			}

			if (token instanceof String) {
				String field = (String) token;

				if (current.has(field)) {
					current = current.get(field);
				} else {
					return null;
				}

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

	private List<Object> tokenize(String path) {
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

	private JsonNode packetToNode(Packet packet) {
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(packet.toMap());
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert packet to JSON tree", e);
		}
	}
}