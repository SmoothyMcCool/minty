package tom.assistant.controller;

public record LlmStatus(RequestProcessingState state, int queuePosition) {

}
