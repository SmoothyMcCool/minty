package tom.assistant.service.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class LlmParseResult {

	private boolean success;
	private LlmResponse structured;
	private String fallbackText;

	public LlmParseResult() {
	}

	public LlmParseResult(boolean success, LlmResponse structured, String fallbackText) {
		this.success = success;
		this.structured = structured;
		this.fallbackText = fallbackText;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	@JsonIgnore
	public boolean isStructured() {
		return structured != null;
	}

	public LlmResponse getStructured() {
		return structured;
	}

	public void setStructured(LlmResponse structured) {
		this.structured = structured;
	}

	public String getFallbackText() {
		return fallbackText;
	}

	public void setFallbackText(String fallbackText) {
		this.fallbackText = fallbackText;
	}

}
