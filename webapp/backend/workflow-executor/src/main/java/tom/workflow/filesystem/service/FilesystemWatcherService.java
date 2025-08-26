package tom.workflow.filesystem.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import tom.api.services.UserService;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.Workflow;
import tom.workflow.repository.WorkflowRepository;
import tom.workflow.service.WorkflowService;

//@Service
public class FilesystemWatcherService {

	private final TaskExecutor taskExecutor;
	private final WorkflowService workflowService;
	private final List<FilesystemMonitor> monitorTasks;
	private final WorkflowRepository workflowRepository;

	public FilesystemWatcherService(@Qualifier("simpleExecutor") SimpleAsyncTaskExecutor taskExecutor,
			WorkflowService workflowService, WorkflowRepository workflowRepository) {
		this.taskExecutor = taskExecutor;
		this.workflowService = workflowService;
		this.workflowRepository = workflowRepository;
		monitorTasks = new ArrayList<>();
	}

	// @PostConstruct
	private void startWatching() {

		List<tom.workflow.repository.Workflow> wfList = workflowRepository.findAllByTriggeredTrue();

		List<Workflow> fsWatchers = wfList.stream().map(workflow -> workflow.toModelWorkflow()).toList();

		FilesystemMonitor monitor = new FilesystemMonitor(fsWatchers, this);
		monitorTasks.add(monitor);
		taskExecutor.execute(monitor);
	}

	// @PreDestroy
	public void shutdown() {
		monitorTasks.forEach(task -> {
			task.stop();
		});
	}

	public void startWorkflowFor(Workflow triggeredWorkflow, Path filename) {
		WorkflowRequest request = new WorkflowRequest();

		List<Map<String, String>> configs = triggeredWorkflow.getWorkflowSteps().stream()
				.map(step -> step.getConfiguration()).toList();
		request.setTaskConfigurationList(configs);
		request.setOutputConfiguration(triggeredWorkflow.getOutputStep().getConfiguration());

		request.getTaskConfigurationList().getFirst().put("File", filename.toString());

		workflowService.executeWorkflow(UserService.DefaultId, request);
	}

}
