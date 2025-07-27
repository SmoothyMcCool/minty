package tom.model;

import java.util.Objects;

public record Assistant(Integer id, String name, String model, Double temperature, String prompt, Integer numFiles,
		Integer processedFiles, AssistantState state, Integer ownerId, boolean shared) {

	public Assistant {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(model, "model cannot be null");
		Objects.requireNonNull(temperature, "temperature cannot be null");
		Objects.requireNonNull(prompt, "prompt cannot be null");
		Objects.requireNonNull(numFiles, "numFiles cannot be null");
		if (processedFiles == null) {
			processedFiles = 0;
		}
		Objects.requireNonNull(state, "state cannot be null");
	}

	private Assistant() {
		this(-1, "null", "null", 0.0, "", 0, 0, AssistantState.READY, 0, false);
	}

	public static Assistant NullAssistant() {
		return new Assistant();
	}

	public static Assistant DefaultAssistant() {
		return new Assistant(0, "default", "LLAMA3_2", 0.0, "", 0, 0, AssistantState.READY, 0, false);
	}

	public boolean Null() {
		return id == -1;
	}
}
