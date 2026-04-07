package tom.api.services.workflow;

import java.util.List;
import java.util.UUID;

import tom.api.WorkflowId;

public class Workflow {

	private WorkflowId id;
	private boolean owned;
	private String name;
	private String description;
	private List<TaskRequest> steps;
	private List<Connection> connections;
	private TaskRequest outputStep;

	private boolean shared;

	public Workflow() {
		id = new WorkflowId(UUID.randomUUID());
		name = "";
		steps = List.of();
		connections = List.of();
		outputStep = null;
		description = "";
		owned = false;
		shared = false;
	}

	public WorkflowId getId() {
		return id;
	}

	public void setId(WorkflowId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<TaskRequest> getSteps() {
		return steps;
	}

	public void setSteps(List<TaskRequest> steps) {
		this.steps = steps;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}

	public TaskRequest getOutputStep() {
		return outputStep;
	}

	public void setOutputStep(TaskRequest outputStep) {
		this.outputStep = outputStep;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isOwned() {
		return owned;
	}

	public void setOwned(boolean owned) {
		this.owned = owned;
	}

	public boolean isShared() {
		return shared;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	public WorkflowDescription generateDescription() {
		return new WorkflowDescription(id.getValue().toString(), name, description, owned);
	}
}
