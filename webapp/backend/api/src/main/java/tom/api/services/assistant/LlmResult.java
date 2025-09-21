package tom.api.services.assistant;

public sealed interface LlmResult permits StringResult, StreamResult {

	public static final LlmResult IN_PROGRESS = new StringResult();
}
