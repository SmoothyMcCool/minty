package tom.ollama.service;

import org.springframework.ai.model.ChatModelDescription;

public enum MintyOllamaModel implements ChatModelDescription {
	LLAMA4("llama4"), LLAMA3_2("llama3.2"), CODELLAMA("codellama"), DEVSTRAL("devstral");

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
