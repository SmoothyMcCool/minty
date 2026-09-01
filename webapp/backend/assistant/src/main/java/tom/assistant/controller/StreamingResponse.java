package tom.assistant.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import tom.api.services.assistant.ChunkType;
import tom.api.services.assistant.LlmMetric;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamingResponse(LlmStatus status, LlmMetric metric, List<String> sources, String name, ChunkType type,
		String content) {
	// Name is used only to identify sub-agents in agentic workflows. It is totally
	// ignored for normal assistant interactions.
}
