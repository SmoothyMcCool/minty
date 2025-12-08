package tom.assistant.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.api.UserId;
import tom.workflow.converters.StringListToStringConverter;

@Entity
public class Assistant {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private String name;
	private String prompt;
	private String model;
	private Double temperature;
	private Integer topK;
	private UserId ownerId;
	private boolean shared;
	private boolean hasMemory;
	@Transient
	private List<DocumentId> associatedDocumentIds;

	@Convert(converter = StringListToStringConverter.class)
	private List<String> tools;

	public Assistant() {
	}

	public Assistant(tom.model.Assistant assistant) {
		this.id = assistant.id().getValue();
		this.name = assistant.name();
		this.prompt = assistant.prompt();
		this.model = assistant.model();
		this.temperature = assistant.temperature();
		this.topK = assistant.topK();
		this.ownerId = assistant.ownerId();
		this.shared = assistant.shared();
		this.hasMemory = assistant.hasMemory();
		this.associatedDocumentIds = new ArrayList<>();
		this.tools = assistant.tools();
	}

	public tom.model.Assistant toTaskAssistant() {
		return new tom.model.Assistant(new AssistantId(id), name, model.toString(), temperature, topK, prompt,
				associatedDocumentIds, tools, ownerId, shared, hasMemory);
	}

	public Assistant updateWith(tom.model.Assistant assistant) {
		setName(assistant.name());
		setPrompt(assistant.prompt());
		setModel(assistant.model());
		setTemperature(assistant.temperature());
		setTopK(assistant.topK());
		setShared(assistant.shared());
		setHasMemory(assistant.hasMemory());
		setAssociatedDocuments(assistant.documentIds());
		setTools(assistant.tools());
		return this;
	}

	public AssistantId getId() {
		return new AssistantId(id);
	}

	public void setId(AssistantId id) {
		this.id = id == null ? null : id.getValue();
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

	public Integer getTopK() {
		return topK;
	}

	public void setTopK(Integer topK) {
		this.topK = topK;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
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
	public List<DocumentId> getAssociatedDocumentIds() {
		return associatedDocumentIds;
	}

	public void setAssociatedDocuments(List<DocumentId> associatedDocumentIds) {
		this.associatedDocumentIds = associatedDocumentIds;
	}

	public List<String> getTools() {
		return tools;
	}

	public void setTools(List<String> tools) {
		this.tools = tools;
	}

}
