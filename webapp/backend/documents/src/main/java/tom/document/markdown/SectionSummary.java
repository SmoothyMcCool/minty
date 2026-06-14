package tom.document.markdown;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SectionSummary(String type, String summary, List<String> keywords, Map<String, JsonNode> entities,
		List<String> queries) {
}
