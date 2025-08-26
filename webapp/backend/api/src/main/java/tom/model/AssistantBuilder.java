package tom.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AssistantBuilder {
	private UUID id;
	private String name;
	private String model;
	private Double temperature;
	private String prompt;
	private UUID ownerId;
	private boolean shared;
	private boolean hasMemory;
	private List<UUID> documentIds;

	public AssistantBuilder id(UUID id) {
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

	public AssistantBuilder documentIds(List<UUID> documentIds) {
		this.documentIds = documentIds;
		return this;
	}

	public AssistantBuilder ownerId(UUID ownerId) {
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
