package tom.workflow.model;

import java.util.List;

import tom.output.OutputTask;

public class Workflow {

	private int id;
	private String name;
	private String description;
	private int ownerId;
	private boolean shared;
	private List<WorkflowStep> workflowSteps;
	private OutputTask outputTask;

	public int getId() {
		return id;
	}

	public void setId(int id) {
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

	public OutputTask getOutputTask() {
		return outputTask;
	}

	public void setOutputTask(OutputTask outputTask) {
		this.outputTask = outputTask;
	}

}
