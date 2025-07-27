package tom.assistant.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.model.AssistantState;

@Entity
public class Assistant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String prompt;
	private String model;
	private Double temperature;
	private Integer numFiles;
	private Integer processedFiles;
	private AssistantState state;
	@JsonIgnore
	private Integer ownerId;
	private boolean shared;

	public Assistant() {
	}

	public Assistant(tom.model.Assistant assistant) {
		this.id = assistant.id();
		this.name = assistant.name();
		this.prompt = assistant.prompt();
		this.model = assistant.model();
		this.temperature = assistant.temperature();
		this.numFiles = assistant.numFiles();
		this.processedFiles = assistant.processedFiles();
		this.ownerId = assistant.ownerId();
		this.state = assistant.state();
		this.shared = assistant.shared();
	}

	public tom.model.Assistant toTaskAssistant() {
		return new tom.model.Assistant(id, name, model.toString(), temperature, prompt, numFiles, processedFiles, state,
				ownerId, shared);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
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

	public Integer getNumFiles() {
		return numFiles;
	}

	public void setNumFiles(Integer numFiles) {
		this.numFiles = numFiles;
	}

	@JsonIgnore
	public Integer getProcessedFiles() {
		return processedFiles;
	}

	public void setProcessedFiles(Integer processedFiles) {
		this.processedFiles = processedFiles;
	}

	public AssistantState getState() {
		return state;
	}

	public void setState(AssistantState state) {
		this.state = state;
	}

	public Integer getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public void fileComplete() {
		processedFiles++;
		if (numFiles == processedFiles) {
			state = AssistantState.READY;
		}
	}
}
