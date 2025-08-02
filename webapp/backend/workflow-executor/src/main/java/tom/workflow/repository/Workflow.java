package tom.workflow.repository;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.workflow.converters.TaskConverter;
import tom.workflow.model.Task;

@Entity
public class Workflow {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String description;
	private int ownerId;
	private boolean shared;
	@Column(columnDefinition = "json")
	@Convert(converter = TaskConverter.class)
	private List<Task> workflowSteps;
	@Convert(converter = TaskConverter.class)
	private Task outputStep;

	public Workflow() {
	}

	public Workflow(tom.workflow.model.Workflow workflow) {
		this.id = workflow.getId();
		this.name = workflow.getName();
		this.description = workflow.getDescription();
		this.ownerId = workflow.getOwnerId();
		this.shared = workflow.isShared();
		this.workflowSteps = workflow.getWorkflowSteps();
		this.outputStep = workflow.getOutputStep();
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(int ownerId) {
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

	public tom.workflow.model.Workflow toModelWorkflow() {
		tom.workflow.model.Workflow workflow = new tom.workflow.model.Workflow();
		workflow.setDescription(getDescription());
		workflow.setId(getId());
		workflow.setName(getName());
		workflow.setOwnerId(getOwnerId());
		workflow.setShared(isShared());
		workflow.setWorkflowSteps(getWorkflowSteps());
		workflow.setOutputStep(getOutputStep());
		return workflow;
	}

}
