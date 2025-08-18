package tom.workflow.service;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;
import tom.workflow.model.TaskRequest;

public class WorkflowTaskWrapper implements Runnable {

	private final Logger logger = LogManager.getLogger(WorkflowTaskWrapper.class);

	private final int taskId;
	private final int stepNumber;
	private final AiTask task;
	private final WorkflowTracker workflowTracker;
	private final TaskRequest taskRequest;
	private List<Map<String, Object>> output = List.of();

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
		try {
			output = task.runTask();
		} catch (Exception e) {
			logger.error("Task " + task.taskName() + " failed with exception: ", e);
		}

		workflowTracker.taskComplete(this);
	}

	public List<Map<String, Object>> getOutput() {
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
