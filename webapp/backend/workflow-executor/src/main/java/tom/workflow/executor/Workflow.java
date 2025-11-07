package tom.workflow.executor;

import java.util.List;
import java.util.UUID;

import tom.api.UserId;

public class Workflow {

	private UUID id;
	private String name;
	private List<TaskRequest> steps;
	private List<Connection> connections;
	private TaskRequest outputStep;
	private String description;
	private UserId ownerId;
	private boolean shared;

	public Workflow() {
		id = UUID.randomUUID();
		name = "";
		steps = List.of();
		connections = List.of();
		outputStep = null;
		description = "";
		ownerId = new UserId(UUID.randomUUID());
		shared = false;
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

	public List<TaskRequest> getSteps() {
		return steps;
	}

	public void setSteps(List<TaskRequest> steps) {
		this.steps = steps;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnectors(List<Connection> connections) {
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

}
