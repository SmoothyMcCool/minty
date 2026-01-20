package tom.workflow.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import tom.NotOwnedException;
import tom.api.UserId;
import tom.config.MintyConfiguration;
import tom.task.model.TaskRequest;
import tom.task.registry.TaskRegistryService;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.executor.WorkflowRunner;
import tom.workflow.model.Workflow;
import tom.workflow.repository.WorkflowRepository;
import tom.workflow.tracking.controller.model.WorkflowState;
import tom.workflow.tracking.service.WorkflowTrackingService;

@Service
public class WorkflowServiceImpl implements WorkflowService {

	private final Logger logger = LogManager.getLogger(WorkflowServiceImpl.class);

	private final WorkflowRepository workflowRepository;
	private final TaskRegistryService taskRegistryService;
	private final WorkflowTrackingService workflowTrackingService;
	private final AsyncTaskExecutor taskExecutor;
	private final Path workflowLoggingFolder;

	public WorkflowServiceImpl(WorkflowRepository workflowRepository, TaskRegistryService taskRegistryService,
			WorkflowTrackingService workflowTrackingService,
			@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor, MintyConfiguration properties) {
		this.workflowRepository = workflowRepository;
		this.taskRegistryService = taskRegistryService;
		this.workflowTrackingService = workflowTrackingService;
		this.taskExecutor = taskExecutor;
		workflowLoggingFolder = properties.getConfig().fileStores().workflowLogs();
		if (workflowLoggingFolder == null) {
			throw new RuntimeException("Workflow log folder location is not defined.");
		}
	}

	@Override
	public String executeWorkflow(UserId userId, WorkflowRequest request) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(request.getId());
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + request.getId() + ". Cannot run.");
			return null;
		}

		Workflow workflow = maybeWorkflow.get().toModelWorkflow();
		List<TaskRequest> steps = workflow.getSteps();

		if (steps.size() != request.getTaskConfigurationList().size()) {
			logger.warn("Expected to get " + steps.size() + " configuration objects, but instead got "
					+ request.getTaskConfigurationList().size());
			return null;
		}

		for (int i = 0; i < steps.size(); i++) {
			steps.get(i).setConfiguration(request.getTaskConfigurationList().get(i));
		}

		if (workflow.getOutputStep() != null) {
			workflow.getOutputStep().setConfiguration(request.getOutputConfiguration());
		}

		WorkflowRunner runner = new WorkflowRunner(userId, workflow, taskRegistryService, workflowTrackingService,
				taskExecutor, workflowLoggingFolder);

		workflowTrackingService.trackWorkflow(runner);

		try {
			runner.start();
			return runner.getWorkflowName();
		} catch (Exception e) {
			logger.warn("Failed to start workflow " + runner.getWorkflowName(), e);
		}

		return null;
	}

	@Override
	public Workflow createWorkflow(UserId userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setId(null);
		return workflowRepository.save(dbWorkflow).toModelWorkflow();
	}

	@Override
	public Workflow updateWorkflow(UserId userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setOwnerId(userId);
		return workflowRepository.save(dbWorkflow).toModelWorkflow();
	}

	@Override
	public List<Workflow> listWorkflows(UserId userId) {
		return workflowRepository.findAllByOwnerIdOrSharedTrue(userId).stream()
				.map(workflow -> workflow.toModelWorkflow()).toList();
	}

	@Override
	public Workflow getWorkflow(UserId userId, UUID workflowId) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(workflowId);
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + workflowId);
			return null;
		}

		Workflow workflow = maybeWorkflow.get().toModelWorkflow();
		if (workflow.isShared() || workflow.getOwnerId().equals(userId)) {
			return workflow;
		}

		return null;
	}

	@Override
	public void deleteWorkflow(UserId userId, UUID workflowId) {
		workflowRepository.deleteById(workflowId);
	}

	@Override
	public void cancelWorkflow(UserId userId, String name) {
		if (!isWorkflowOwned(userId, name)) {
			throw new NotOwnedException("User does not own this workflow.");
		}
		workflowTrackingService.cancelWorkflow(userId, name);
	}

	@Override
	public boolean isAllowedToExecute(UUID workflowId, UserId userId) {
		return this.getWorkflow(userId, workflowId) != null;
	}

	@Override
	public boolean isWorkflowOwned(UUID workflowId, UserId userId) {
		Workflow workflow = getWorkflow(userId, workflowId);
		if (workflow == null) {
			return false;
		}
		return workflow.getOwnerId().equals(userId);
	}

	@Override
	public boolean isWorkflowOwned(UserId userId, String name) {
		List<WorkflowState> runningWorkflows = workflowTrackingService.getWorkflowList(userId);
		boolean found = false;
		for (WorkflowState workflow : runningWorkflows) {
			if (workflow.name().equals(name)) {
				found = true;
				break;
			}
		}
		return found;
	}

}
