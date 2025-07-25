package tom.task.filesystem.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.controller.TaskRequest;
import tom.task.executor.service.TaskExecutionService;
import tom.task.model.StandaloneTask;
import tom.task.repository.StandaloneTaskRepository;
import tom.task.taskregistry.TaskRegistryService;

@Service
public class FilesystemWatcherService {

	private final TaskExecutor taskExecutor;
	private final TaskRegistryService taskRegistryService;
	private final TaskExecutionService taskExecutionService;
	private final List<FilesystemMonitor> monitorTasks;
	private final StandaloneTaskRepository standaloneTaskRepository;

	public FilesystemWatcherService(@Qualifier("simpleExecutor") SimpleAsyncTaskExecutor taskExecutor,
			TaskRegistryService taskRegistryService, TaskExecutionService taskExecutionService,
			StandaloneTaskRepository standaloneTaskRepository) {
		this.taskExecutor = taskExecutor;
		this.taskRegistryService = taskRegistryService;
		this.taskExecutionService = taskExecutionService;
		this.standaloneTaskRepository = standaloneTaskRepository;
		monitorTasks = new ArrayList<>();
	}

	@PostConstruct
	private void startWatching() {

		Iterable<StandaloneTask> fsWatchers = standaloneTaskRepository.findAllByTriggeredTrue();

		FilesystemMonitor monitor = new FilesystemMonitor(fsWatchers, this);
		monitorTasks.add(monitor);
		taskExecutor.execute(monitor);
	}

	@PreDestroy
	public void shutdown() {
		monitorTasks.forEach(task -> {
			task.stop();
		});
	}

	public void startTaskFor(StandaloneTask triggeredTask, Path filename) {
		TaskRequest request = new TaskRequest();
		request.setConfiguration(triggeredTask.getTaskTemplate().getConfiguration());
		request.setName(triggeredTask.getTaskTemplate().getName());
		request.getConfiguration().put("File", filename.toString());

		AiTask task = taskRegistryService.newTask(0, request);

		TaskRequest outputRequest = new TaskRequest();
		request.setConfiguration(triggeredTask.getOutputTemplate().getConfiguration());
		request.setName(triggeredTask.getOutputTemplate().getName());
		OutputTask outputTask = taskRegistryService.newOutputTask(0, outputRequest);

		taskExecutionService.executeTask(task, outputTask);
	}

}
