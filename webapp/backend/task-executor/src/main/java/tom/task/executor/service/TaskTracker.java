package tom.task.executor.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.output.OutputTask;

public class TaskTracker {

	private final Logger logger = LogManager.getLogger(TaskTracker.class);

	private final TaskExecutionService taskExecutionService;
	private final Instant startTime;
	private Instant endTime;
	private final AiTaskWrapper initialTask;
	private boolean taskComplete;

	public TaskTracker(AiTaskWrapper initialTask, TaskExecutionService taskExecutionService) {
		startTime = Instant.now();
		taskComplete = false;
		this.initialTask = initialTask;
		this.taskExecutionService = taskExecutionService;
	}

	public synchronized void taskComplete() {
		if (taskComplete) {
			return;
		}

		if (initialTask.isComplete()) {
			taskComplete = true;
			endTime = Instant.now();
			taskExecutionService.reportTaskComplete(this);
		}

	}

	public String getTaskName() {
		return initialTask.taskName();
	}

	public void generateReport(String resultsDir) {
		Map<String, Object> results = initialTask.getResult();
		results.put("startTime", startTime);
		results.put("endTime", endTime);

		OutputTask outputTask = initialTask.getOutputRenderer();
		try {
			outputTask.execute(results);
		} catch (IOException e) {
			logger.error("Failed to generate output for " + initialTask.taskName());
		}
	}

}
