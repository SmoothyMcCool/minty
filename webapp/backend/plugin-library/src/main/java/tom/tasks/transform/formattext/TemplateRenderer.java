package tom.tasks.transform.formattext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import tom.api.task.Packet;

public class TemplateRenderer {

	private static final Pattern FIELD_PATTERN = Pattern.compile("\\{([^{}#/][^{}]*)}");
	private static final Pattern LOOP_START = Pattern.compile("\\{#([^{}]+)}");
	private static final Pattern LOOP_END = Pattern.compile("\\{/([^{}]+)}");

	public List<String> render(Packet packet, String template) {
		List<String> lines = Arrays.asList(template.split("\\R"));
		return renderBlock(packet, null, lines);
	}

	private List<String> renderBlock(Packet packet, JsonNode context, List<String> lines) {
		List<String> output = new ArrayList<>();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			Matcher startMatcher = LOOP_START.matcher(line);

			if (startMatcher.find()) {
				String path = startMatcher.group(1).strip();
				List<String> block = new ArrayList<>();
				int depth = 1;
				i++;

				while (i < lines.size()) {
					String candidate = lines.get(i);

					Matcher nestedStart = LOOP_START.matcher(candidate);
					Matcher nestedEnd = LOOP_END.matcher(candidate);

					if (nestedStart.find() && nestedStart.group(1).equals(path)) {
						depth++;
					}

					if (nestedEnd.find() && nestedEnd.group(1).equals(path)) {
						depth--;

						if (depth == 0) {
							break;
						}
					}

					block.add(candidate);
					i++;
				}

				List<JsonNode> items = resolveArray(packet, context, path);
				for (JsonNode item : items) {
					output.addAll(renderBlock(packet, item, block));
				}

			} else {
				output.add(resolveFields(packet, context, line));
			}
		}

		return output;
	}

	private List<JsonNode> resolveArray(Packet packet, JsonNode context, String path) {
		if (context != null && context.has(path) && context.get(path).isArray()) {
			List<JsonNode> list = new ArrayList<>();
			context.get(path).forEach(list::add);
			return list;
		}
		return packet.resolveArray(path);
	}

	private String resolveFields(Packet packet, JsonNode context, String line) {
		Matcher matcher = FIELD_PATTERN.matcher(line);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String field = matcher.group(1).strip();
			String value = null;

			if (context != null && context.has(field)) {
				value = context.get(field).asText();
			}

			if (value == null) {
				value = packet.resolve(field);
			}

			if (value == null) {
				value = matcher.group(0);
			}

			matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
		}

		matcher.appendTail(sb);
		return sb.toString();
	}
}