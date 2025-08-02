package tom.workflow.service;

import java.util.List;
import java.util.Map;

import tom.task.AiTask;
import tom.workflow.model.TaskRequest;

public class WorkflowTaskWrapper implements Runnable {

	private final int taskId;
	private final int stepNumber;
	private final AiTask task;
	private final WorkflowTracker workflowTracker;
	private final TaskRequest taskRequest;
	private List<Map<String, String>> output = List.of();

	public WorkflowTaskWrapper(int taskId, int stepNumber, AiTask task, WorkflowTracker workflowTracker,
			TaskRequest taskRequest) {
		this.taskId = taskId;
		this.stepNumber = stepNumber;
		this.task = task;
		this.workflowTracker = workflowTracker;
		this.taskRequest = taskRequest;
	}

	@Override
	public void run() {
		output = task.runWorkflow();
		workflowTracker.taskComplete(this);
	}

	public List<Map<String, String>> getOutput() {
		return output;
	}

	public int getTaskId() {
		return taskId;
	}

	public TaskRequest getRequest() {
		return taskRequest;
	}

	public int getStepNumber() {
		return stepNumber;
	}

	public Map<String, Object> getResult() {
		return task.getResult();
	}
}
