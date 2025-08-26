package tom.assistant.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class Assistant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private String name;
	private String prompt;
	private String model;
	private Double temperature;
	private UUID ownerId;
	private boolean shared;
	private boolean hasMemory;
	@Transient
	private List<UUID> associatedDocumentIds;

	public Assistant() {
	}

	public Assistant(tom.model.Assistant assistant) {
		this.id = assistant.id();
		this.name = assistant.name();
		this.prompt = assistant.prompt();
		this.model = assistant.model();
		this.temperature = assistant.temperature();
		this.ownerId = assistant.ownerId();
		this.shared = assistant.shared();
		this.hasMemory = assistant.hasMemory();
		this.associatedDocumentIds = new ArrayList<>();
	}

	public tom.model.Assistant toTaskAssistant() {
		return new tom.model.Assistant(id, name, model.toString(), temperature, prompt, associatedDocumentIds, ownerId,
				shared, hasMemory);
	}

	public Assistant updateWith(tom.model.Assistant assistant) {
		setName(assistant.name());
		setPrompt(assistant.prompt());
		setModel(assistant.model());
		setTemperature(assistant.temperature());
		setShared(assistant.shared());
		setHasMemory(assistant.hasMemory());
		setAssociatedDocuments(assistant.documentIds());
		return this;
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrompt() {
		return prompt;
	}

	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public boolean isHasMemory() {
		return hasMemory;
	}

	public void setHasMemory(boolean hasMemory) {
		this.hasMemory = hasMemory;
	}

	@Transient
	public List<UUID> getAssociatedDocumentIds() {
		return associatedDocumentIds;
	}

	public void setAssociatedDocuments(List<UUID> associatedDocumentIds) {
		this.associatedDocumentIds = associatedDocumentIds;
	}

}
