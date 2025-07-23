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
import tom.task.filesystem.repository.FilesystemWatcher;
import tom.task.filesystem.repository.FilesystemWatcherRepository;
import tom.task.filesystem.repository.TriggeredTask;
import tom.task.taskregistry.TaskRegistryService;

@Service
public class FilesystemWatcherService {

	private final FilesystemWatcherRepository filesystemWatcherRepository;
	private final TaskExecutor taskExecutor;
	private final TaskRegistryService taskRegistryService;
	private final TaskExecutionService taskExecutionService;
	private final List<FilesystemMonitor> monitorTasks;

	public FilesystemWatcherService(FilesystemWatcherRepository filesystemWatcherRepository,
			@Qualifier("simpleExecutor") SimpleAsyncTaskExecutor taskExecutor, TaskRegistryService taskRegistryService,
			TaskExecutionService taskExecutionService) {
		this.filesystemWatcherRepository = filesystemWatcherRepository;
		this.taskExecutor = taskExecutor;
		this.taskRegistryService = taskRegistryService;
		this.taskExecutionService = taskExecutionService;
		monitorTasks = new ArrayList<>();
	}

	@PostConstruct
	private void startWatching() {

		Iterable<FilesystemWatcher> fsWatchers = filesystemWatcherRepository.findAll();

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

	public void startTaskFor(FilesystemWatcher watcher, Path filename) {
		watcher.getRequest().getData().put("File", filename.toString());
		AiTask task = taskRegistryService.newTask(0, watcher.getRequest());

		OutputTask outputTask = taskRegistryService.newOutputTask(0, watcher.getRequest());

		taskExecutionService.executeTask(task, outputTask);
	}

	public FilesystemWatcher newFilesystemWatcher(TriggeredTask newTask) {
		FilesystemWatcher watcher = new FilesystemWatcher();
		watcher.setId(null);
		watcher.setName(newTask.getName());
		watcher.setDescription(newTask.getDescription());
		watcher.setLocationToWatch(newTask.getDirectory());

		TaskRequest request = new TaskRequest();
		request.setRequest(newTask.getTask());
		request.setData(newTask.getDefaultConfig());
		watcher.setRequest(request);

		return filesystemWatcherRepository.save(watcher);
	}

}
