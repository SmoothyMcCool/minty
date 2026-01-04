package tom.api.services.assistant;

public sealed interface LlmResult permits StringResult, StreamResult {

	public static final LlmResult IN_PROGRESS = new StringResult();
	public static final LlmResult THINKING_STREAM_NOT_READY = new StreamResult();
}
