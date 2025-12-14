package tom.api.model;

import java.util.List;
import java.util.Objects;

import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.api.UserId;

public record Assistant(AssistantId id, String name, String model, Double temperature, Integer topK, String prompt,
		List<DocumentId> documentIds, List<String> tools, UserId ownerId, boolean shared, boolean hasMemory) {

	public Assistant {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(model, "model cannot be null");
		Objects.requireNonNull(temperature, "temperature cannot be null");
		Objects.requireNonNull(topK, "topK cannot be null");
		Objects.requireNonNull(prompt, "prompt cannot be null");
		Objects.requireNonNull(documentIds, "documentIds cannot be null");
		Objects.requireNonNull(tools, "tools cannot be null");
	}

}
