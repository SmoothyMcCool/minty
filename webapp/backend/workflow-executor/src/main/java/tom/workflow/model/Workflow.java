package tom.workflow.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Workflow {

	private UUID id;
	private String name = "";
	private String description = "";
	private UUID ownerId;
	private boolean shared = false;
	private List<Task> workflowSteps = null;
	private Task outputStep = null;
	private boolean triggered = false;
	private String watchLocation = "";

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public List<Task> getWorkflowSteps() {
		return workflowSteps;
	}

	public void setWorkflowSteps(List<Task> workflowSteps) {
		this.workflowSteps = workflowSteps;
	}

	public Task getOutputStep() {
		return outputStep;
	}

	public void setOutputStep(Task outputStep) {
		this.outputStep = outputStep;
	}

	@JsonIgnore
	public int numSteps() {
		return workflowSteps.size();
	}

	public String getWatchLocation() {
		return watchLocation;
	}

	public void setWatchLocation(String watchLocation) {
		this.watchLocation = watchLocation;
	}

	public boolean isTriggered() {
		return triggered;
	}

	public void setTriggered(boolean triggered) {
		this.triggered = triggered;
	}
}
