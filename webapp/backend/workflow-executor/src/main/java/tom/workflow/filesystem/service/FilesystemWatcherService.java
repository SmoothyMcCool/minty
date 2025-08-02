package tom.workflow.filesystem.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import tom.result.service.ResultService;
import tom.task.taskregistry.TaskRegistryService;
import tom.workflow.model.Workflow;
import tom.workflow.repository.WorkflowRepository;

//@Service
public class FilesystemWatcherService {

	//private final TaskExecutor taskExecutor;
	//private final TaskRegistryService taskRegistryService;
	//private final ResultService taskExecutionService;
	private final List<FilesystemMonitor> monitorTasks;
	//private final WorkflowRepository workflowRepository;

	public FilesystemWatcherService(@Qualifier("simpleExecutor") SimpleAsyncTaskExecutor taskExecutor,
			TaskRegistryService taskRegistryService, ResultService taskExecutionService,
			WorkflowRepository workflowRepository) {
		//this.taskExecutor = taskExecutor;
		//this.taskRegistryService = taskRegistryService;
		//this.taskExecutionService = taskExecutionService;
		//this.workflowRepository = workflowRepository;
		monitorTasks = new ArrayList<>();
	}

	@PostConstruct
	private void startWatching() {

		// Iterable<Workflow> fsWatchers = workflowRepository.findAllByTriggeredTrue();

		// FilesystemMonitor monitor = new FilesystemMonitor(fsWatchers, this);
		// monitorTasks.add(monitor);
		// taskExecutor.execute(monitor);
	}

	@PreDestroy
	public void shutdown() {
		monitorTasks.forEach(task -> {
			task.stop();
		});
	}

	public void startTaskFor(Workflow triggeredWorkflow, Path filename) {
		/*WorkflowRequest request = new WorkflowRequest();
		request.setConfiguration(triggeredWorkflow.getTaskTemplate().getConfiguration());
		request.setName(triggeredWorkflow.getTaskTemplate().getName());
		request.getConfiguration().put("File", filename.toString());

		AiTask task = taskRegistryService.newTask(0, request);

		TaskRequest outputRequest = new TaskRequest();
		request.setConfiguration(triggeredTask.getOutputTemplate().getConfiguration());
		request.setName(triggeredTask.getOutputTemplate().getName());
		OutputTask outputTask = taskRegistryService.newOutputTask(0, outputRequest);

		taskExecutionService.executeTask(task, outputTask);*/
	}

}
