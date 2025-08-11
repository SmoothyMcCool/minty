package tom.ollama.service;

import org.springframework.ai.model.ChatModelDescription;

public enum MintyOllamaModel implements ChatModelDescription {
	GEMMA3_12B("gemma3:12b"), GPT_OSS("gpt-oss"), LLAMA4("llama4"), LLAMA3_2("llama3.2"), CODELLAMA("codellama"),
	TINYLLAMA("tinyllama"), DEVSTRAL("devstral"), NOMIC_EMBED_TEXT("nomic-embed-text");

	private final String id;

	MintyOllamaModel(String id) {
		this.id = id;
	}

	public String id() {
		return this.id;
	}

	@Override
	public String getName() {
		return this.id;
	}
}
