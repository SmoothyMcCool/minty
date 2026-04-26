package tom.document.markdown;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record SectionEntities(List<String> requirements, List<String> interfaces, List<String> components,
		List<String> parameters) {
}