package tom.workflow.service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.controller.TaskRequest;
import tom.task.taskregistry.TaskRegistryService;
import tom.workflow.model.Workflow;
import tom.workflow.model.WorkflowStep;

public class WorkflowTracker {

	private final Logger logger = LogManager.getLogger(WorkflowTracker.class);

	private final TaskRegistryService taskRegistryService;
	private final AsyncTaskExecutor taskExecutor;
	private Instant startTime;
	private Instant endTime;
	private final Workflow workflow;
	private boolean taskComplete;
	private int userId;
	private UUID uuid = UUID.randomUUID();
	private Map<String, Object> results = new HashMap<>();
	private int stepTaskCount = 0;
	private Map<Integer, WorkflowTaskWrapper> pendingTasks = new HashMap<>();

	public WorkflowTracker(int userId, Workflow workflow, TaskRegistryService taskRegistryService,
			AsyncTaskExecutor taskExecutor) {
		startTime = Instant.now();
		taskComplete = false;
		this.userId = userId;
		this.workflow = workflow;
		this.taskRegistryService = taskRegistryService;
		this.taskExecutor = taskExecutor;
	}

	public synchronized void workflowComplete() {
		// Ensure we only generate output once.
		if (taskComplete) {
			return;
		}

		taskComplete = true;
		endTime = Instant.now();
		OutputTask outputTask = workflow.getOutputTask();
		results.put("startTime", startTime);
		results.put("endTime", endTime);

		try {
			outputTask.execute(results);
		} catch (IOException e) {
			logger.error("Failed to generate output for " + workflow.getName());
		}
	}

	public String getTaskName() {
		return workflow.getName() + "-" + uuid;
	}

	public void runFirstTask() {
		logger.info("Starting workflow " + workflow.getName());

		startTime = Instant.now();
		if (workflow.getWorkflowSteps().isEmpty()) {
			// That was quick. I guess we're done.
			endTime = Instant.now();
			return;
		}

		WorkflowStep step = workflow.getWorkflowSteps().get(0);
		TaskRequest taskRequest = new TaskRequest(step.getTaskName(), step.getConfig());
		AiTask currentTask = taskRegistryService.newTask(userId, taskRequest);

		int stepNumber = 0;
		WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(stepTaskCount++, stepNumber, currentTask, this,
				taskRequest);

		pendingTasks.put(stepTaskCount, wrapper);

		taskExecutor.execute(wrapper);
	}

	@SuppressWarnings("unchecked")
	public synchronized void taskComplete(WorkflowTaskWrapper completedTask) {

		// Before doing anything, grab the result output of the task that just finished.
		// If the number of tasks in the step is > 1, then ensure we append the data as
		// array data.
		Map<String, Object> result = completedTask.getResult();
		String key = completedTask.getRequest().getRequest().replaceAll("\\s+", "");
		if (!results.containsKey(key)) { // The key doesn't exist, so just straight up add the result
			results.put(key, result);
		} else {
			if (results.get(key) instanceof List) { // The key exists, and is already an array. Append the current
													// result.
				((List<Map<String, Object>>) results.get(key)).add(result);
			} else { // The key exists, but its not an array of results yet. Make it an array and
						// append the current result.
				Map<String, Object> singleResult = (Map<String, Object>) results.get(key);
				results.put(key, List.of(singleResult, result));
			}
		}

		// Remove the just-completed task from the list of pending tasks.
		pendingTasks.remove(completedTask.getTaskId());

		// If the list of pending tasks is empty, and there are no more tasks to
		// generate from the just completed task (or the task is the last one in the
		// list of steps),
		// then the workflow should stop.
		List<Map<String, String>> output = completedTask.getOutput();
		int currentWorkflowStep = completedTask.getStepNumber();
		int lastWorkflowStep = workflow.getWorkflowSteps().size() - 1;
		if (pendingTasks.isEmpty() && (output.isEmpty() || currentWorkflowStep == lastWorkflowStep)) {
			workflowComplete();
			return;
		}

		int nextStep = currentWorkflowStep++;
		WorkflowStep step = workflow.getWorkflowSteps().get(nextStep);
		TaskRequest taskRequest = new TaskRequest(step.getTaskName(), step.getConfig());

		for (Map<String, String> prevOut : completedTask.getOutput()) {
			AiTask task = taskRegistryService.newTask(userId, taskRequest);
			task.setInput(prevOut);

			WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(stepTaskCount++, nextStep, task, this, taskRequest);
			pendingTasks.put(stepTaskCount, wrapper);
			taskExecutor.execute(wrapper);
		}

	}

}
