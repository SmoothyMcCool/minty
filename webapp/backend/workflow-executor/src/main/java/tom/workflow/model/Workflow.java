package tom.workflow.model;

import java.util.List;

import tom.task.model.Task;

public class Workflow {

	private int id;
	private String name;
	private String description;
	private int ownerId;
	private boolean shared;
	private List<Task> workflowSteps;
	private Task outputStep;

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

}
