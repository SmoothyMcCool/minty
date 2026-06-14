package tom.document.markdown;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SectionResult(int index, String title, SectionSummary summary) {
}
