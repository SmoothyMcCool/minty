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
		return renderBlock(packet, null, template).strip();
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
			String block = matcher.group(2).replaceFirst("^\\r?\\n", "");

			JsonNode node = resolveNode(packet, context, path);
			StringBuilder replacement = new StringBuilder();

			if (node != null && node.isArray()) {
				for (JsonNode element : node) {
					replacement.append(renderBlock(packet, element, block));
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
			String path = matcher.group(1).strip();
			JsonNode node = resolveNode(packet, context, path);

			String value = (node != null && !node.isNull()) ? (node.isValueNode() ? node.asText() : node.toString())
					: matcher.group(0);

			matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
		}

		matcher.appendTail(sb);
		return sb.toString();
	}

	private JsonNode resolveNode(Packet packet, JsonNode context, String path) {
		if (context != null) {
			JsonNode node = resolveAgainstContext(context, path);
			if (node != null) {
				return node;
			}
		}
		return packet.resolveNode(path);
	}

	private JsonNode resolveAgainstContext(JsonNode context, String path) {
		List<String> parts = tokenize(path);
		JsonNode current = context;

		for (String part : parts) {
			if (current == null)
				return null;

			if (part.matches("\\d+")) {
				int index = Integer.parseInt(part);
				if (!current.isArray() || index >= current.size())
					return null;
				current = current.get(index);
			} else {
				if (!current.has(part))
					return null;
				current = current.get(part);
			}
		}
		return current;
	}

	private List<String> tokenize(String path) {
		List<String> tokens = new ArrayList<>();
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
				// strip quotes for ['Multi Word'] style
				inside = inside.replaceAll("^['\"]|['\"]$", "");
				tokens.add(inside);
				i = end + 1;
				continue;
			}

			int start = i;
			while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[')
				i++;
			tokens.add(path.substring(start, i));
		}

		return tokens;
	}
}