package tom.workflow.repository;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.api.UserId;
import tom.repository.converter.UserIdConverter;
import tom.workflow.converters.TaskRequestConverter;
import tom.workflow.converters.ConnectionConverter;
import tom.workflow.model.Connection;
import tom.workflow.model.TaskRequest;

@Entity
public class Workflow {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	private String name;
	private String description;
	@Convert(converter = UserIdConverter.class)
	private UserId ownerId;
	private boolean shared;
	@Column(columnDefinition = "json")
	@Convert(converter = TaskRequestConverter.class)
	private List<TaskRequest> steps;
	@Convert(converter = TaskRequestConverter.class)
	private TaskRequest outputStep;
	@Convert(converter = ConnectionConverter.class)
	private List<Connection> connections;

	public Workflow() {
	}

	public Workflow(tom.workflow.model.Workflow workflow) {
		this.id = workflow.getId();
		this.name = workflow.getName();
		this.description = workflow.getDescription();
		this.ownerId = workflow.getOwnerId();
		this.shared = workflow.isShared();
		this.connections = workflow.getConnections();
		this.steps = workflow.getSteps();
		this.outputStep = workflow.getOutputStep();
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

	public List<TaskRequest> getSteps() {
		return steps;
	}

	public void setSteps(List<TaskRequest> steps) {
		this.steps = steps;
	}

	public TaskRequest getOutputStep() {
		return outputStep;
	}

	public void setOutputStep(TaskRequest outputStep) {
		this.outputStep = outputStep;
	}

	public List<Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}

	public tom.workflow.model.Workflow toModelWorkflow() {
		tom.workflow.model.Workflow workflow = new tom.workflow.model.Workflow();
		workflow.setConnections(getConnections());
		workflow.setDescription(getDescription());
		workflow.setId(getId());
		workflow.setName(getName());
		workflow.setOwnerId(getOwnerId());
		workflow.setShared(isShared());
		workflow.setOutputStep(getOutputStep());
		workflow.setSteps(getSteps());
		return workflow;
	}

}
