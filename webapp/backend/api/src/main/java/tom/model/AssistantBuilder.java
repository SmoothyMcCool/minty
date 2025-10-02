package tom.model;

import java.util.List;
import java.util.Objects;

import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.api.UserId;

public class AssistantBuilder {
	private AssistantId id;
	private String name;
	private String model;
	private Double temperature;
	private String prompt;
	private UserId ownerId;
	private boolean shared;
	private boolean hasMemory;
	private List<DocumentId> documentIds;

	public AssistantBuilder id(AssistantId id) {
		this.id = id;
		return this;
	}

	public AssistantBuilder name(String name) {
		this.name = name;
		return this;
	}

	public AssistantBuilder model(String model) {
		this.model = model;
		return this;
	}

	public AssistantBuilder temperature(Double temperature) {
		this.temperature = temperature;
		return this;
	}

	public AssistantBuilder prompt(String prompt) {
		this.prompt = prompt;
		return this;
	}

	public AssistantBuilder documentIds(List<DocumentId> documentIds) {
		this.documentIds = documentIds;
		return this;
	}

	public AssistantBuilder ownerId(UserId ownerId) {
		this.ownerId = ownerId;
		return this;
	}

	public AssistantBuilder shared(boolean shared) {
		this.shared = shared;
		return this;
	}

	public AssistantBuilder hasMemory(boolean hasMemory) {
		this.hasMemory = hasMemory;
		return this;
	}

	public Assistant build() {
		return new Assistant(Objects.requireNonNull(id, "id must not be null"),
				Objects.requireNonNull(name, "name must not be null"),
				Objects.requireNonNull(model, "model must not be null"),
				Objects.requireNonNull(temperature, "temperature must not be null"),
				Objects.requireNonNull(prompt, "prompt must not be null"),
				Objects.requireNonNull(documentIds, "documentIds must not be null"),
				Objects.requireNonNull(ownerId, "ownerId must not be null"), shared, hasMemory);
	}
}
