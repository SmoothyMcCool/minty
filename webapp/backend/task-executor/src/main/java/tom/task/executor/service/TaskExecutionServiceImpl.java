package tom.task.executor.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import tom.output.OutputTask;
import tom.task.AiTask;

@Service
public class TaskExecutionServiceImpl implements TaskExecutionService {

	private final Logger logger = LogManager.getLogger(TaskExecutionServiceImpl.class);

	private final AsyncTaskExecutor taskExecutor;
	private final Map<String, TaskTracker> processTrackerMap;

	@Value("${results.location}")
	private String resultsDir;

	@Value("${pugTemplates}")
	private String pugFileLocation;

	public TaskExecutionServiceImpl(@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		processTrackerMap = new HashMap<>();
	}

	@Override
	public String executeTask(AiTask task, OutputTask outputTask) {
		AiTaskWrapper wrappedTask = new AiTaskWrapper(task, outputTask);
		wrappedTask.setExecutor(taskExecutor);

		trackTask(wrappedTask);
		taskExecutor.submit(wrappedTask);

		return task.taskName();
	}

	@Override
	public List<String> getAvailableResults() throws IOException {
		Path dir = Paths.get(resultsDir);
		List<String> results = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (Files.isRegularFile(entry)) {
					results.add(entry.getFileName().toString());
				}
			}
		}
		return results;
	}

	@Override
	public String getResult(String taskName) throws IOException {
		Path dir = Paths.get(resultsDir);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (entry.getFileName().endsWith(taskName)) {
					return Files.readString(entry);
				}
			}
		}
		return "Result not found.";
	}

	@Override
	public boolean deleteResult(String resultName) throws IOException {
		Path dir = Paths.get(resultsDir);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (Path entry : stream) {
				if (entry.getFileName().endsWith(resultName)) {
					Files.delete(entry);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void reportTaskComplete(TaskTracker tracker) {
		logger.info("reportTaskComplete: Task " + tracker.getTaskName() + " is complete.");
		tracker.generateReport(resultsDir);
	}

	private void trackTask(AiTaskWrapper task) {
		TaskTracker tracker = new TaskTracker(task, this);
		processTrackerMap.put(tracker.getTaskName(), tracker);
		task.setResultTracker(tracker);
	}
}
