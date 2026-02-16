package tom.config.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LlmEngine {
	Ollama("Ollama"), vllm("vllm");

	private final String value;

	LlmEngine(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}
}
