package tom.workflow.repository;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import tom.workflow.model.WorkflowStep;

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
	private List<WorkflowStep> workflowSteps;

	public Workflow(tom.workflow.model.Workflow workflow) {
		this.id = workflow.getId();
		this.name = workflow.getName();
		this.description = workflow.getDescription();
		this.ownerId = workflow.getOwnerId();
		this.shared = workflow.isShared();
		this.workflowSteps = workflow.getWorkflowSteps();
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

	public List<WorkflowStep> getWorkflowSteps() {
		return workflowSteps;
	}

	public void setWorkflowSteps(List<WorkflowStep> workflowSteps) {
		this.workflowSteps = workflowSteps;
	}

	public tom.workflow.model.Workflow toModelWorkflow() {
		tom.workflow.model.Workflow workflow = new tom.workflow.model.Workflow();
		workflow.setDescription(getDescription());
		workflow.setId(getId());
		workflow.setName(getName());
		workflow.setOwnerId(getOwnerId());
		workflow.setShared(isShared());
		workflow.setWorkflowSteps(getWorkflowSteps());
		return workflow;
	}

}
