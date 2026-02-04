package tom.api.services.assistant;

public sealed interface LlmResult permits StringResult, StreamResult {

}
