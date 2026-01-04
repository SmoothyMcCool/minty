package tom.assistant.controller;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import tom.api.services.assistant.LlmMetric;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreamingResponse(LlmStatus status, LlmMetric metric, List<String> sources, String content) {

}
