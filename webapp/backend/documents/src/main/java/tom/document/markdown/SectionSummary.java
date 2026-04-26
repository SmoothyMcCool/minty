package tom.document.markdown;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SectionSummary(boolean insufficient, String type, String summary, List<String> keywords,
		SectionEntities entities, List<String> queries) {
}
