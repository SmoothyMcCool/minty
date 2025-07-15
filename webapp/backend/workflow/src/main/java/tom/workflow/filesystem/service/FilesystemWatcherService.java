package tom.workflow.filesystem.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import tom.task.AiTask;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.executor.service.WorkflowExecutionService;
import tom.workflow.filesystem.repository.FilesystemWatcher;
import tom.workflow.filesystem.repository.FilesystemWatcherRepository;
import tom.workflow.filesystem.repository.TriggeredWorkflowTask;
import tom.workflow.taskregistry.TaskRegistryService;

@Service
public class FilesystemWatcherService {

	private final FilesystemWatcherRepository filesystemWatcherRepository;
	private final TaskExecutor taskExecutor;
	private final TaskRegistryService taskRegistryService;
	private final WorkflowExecutionService workflowExecutionService;
	private final List<FilesystemMonitor> monitorTasks;

	public FilesystemWatcherService(FilesystemWatcherRepository filesystemWatcherRepository, @Qualifier("simpleExecutor") SimpleAsyncTaskExecutor taskExecutor,
			TaskRegistryService taskRegistryService, WorkflowExecutionService workflowExecutionService) {
		this.filesystemWatcherRepository = filesystemWatcherRepository;
		this.taskExecutor = taskExecutor;
		this.taskRegistryService = taskRegistryService;
		this.workflowExecutionService = workflowExecutionService;
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
		watcher.getRequest().getData().put("file", filename.toString());
		AiTask task = taskRegistryService.newTask(0, watcher.getRequest());
		workflowExecutionService.executeTask(task);
	}

	public FilesystemWatcher newFilesystemWatcher(TriggeredWorkflowTask newTask) {
		FilesystemWatcher watcher = new FilesystemWatcher();
		watcher.setId(null);
		watcher.setName(newTask.getName());
		watcher.setDescription(newTask.getDescription());
		watcher.setLocationToWatch(newTask.getDirectory());

		WorkflowRequest request = new WorkflowRequest();
		request.setRequest(newTask.getWorkflow());
		request.setData(newTask.getDefaultConfig());
		watcher.setRequest(request);

		return filesystemWatcherRepository.save(watcher);
	}

}
