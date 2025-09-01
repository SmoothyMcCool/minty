package tom.workflow.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import tom.conversation.service.ConversationServiceInternal;
import tom.task.taskregistry.TaskRegistryService;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.Task;
import tom.workflow.model.Workflow;
import tom.workflow.repository.WorkflowRepository;
import tom.workflow.tracking.service.WorkflowTrackingService;

@Service
public class WorkflowServiceImpl implements WorkflowService {

	private final Logger logger = LogManager.getLogger(WorkflowServiceImpl.class);

	private final WorkflowRepository workflowRepository;
	private final TaskRegistryService taskRegistryService;
	private final WorkflowTrackingService workflowTrackingService;
	private final ConversationServiceInternal conversationService;
	private final AsyncTaskExecutor taskExecutor;

	public WorkflowServiceImpl(WorkflowRepository workflowRepository, TaskRegistryService taskRegistryService,
			WorkflowTrackingService workflowTrackingService,ConversationServiceInternal conversationService,
			@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
		this.workflowRepository = workflowRepository;
		this.taskRegistryService = taskRegistryService;
		this.workflowTrackingService = workflowTrackingService;
		this.taskExecutor = taskExecutor;
		this.conversationService = conversationService;
	}

	@Override
	public String executeWorkflow(UUID userId, WorkflowRequest request) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(request.getId());
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + request.getId() + ". Cannot run.");
			return null;
		}

		Workflow workflow = maybeWorkflow.get().toModelWorkflow();
		List<Task> steps = workflow.getWorkflowSteps();

		if (steps.size() != request.getTaskConfigurationList().size()) {
			logger.warn("Expected to get " + steps.size() + " configuration objects, but instead got "
					+ request.getTaskConfigurationList().size());
			return null;
		}

		for (int i = 0; i < steps.size(); i++) {
			steps.get(i).setConfiguration(request.getTaskConfigurationList().get(i));
		}
		workflow.getOutputStep().setConfiguration(request.getOutputConfiguration());

		WorkflowRunner runner = new WorkflowRunner(userId, workflow, taskRegistryService, conversationService,
				workflowTrackingService, taskExecutor);
		workflowTrackingService.trackWorkflow(runner);
		runner.runFirstTask();

		return runner.getWorkflowName();
	}

	@Override
	public Workflow createWorkflow(UUID userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setId(null);
		return workflowRepository.save(dbWorkflow).toModelWorkflow();
	}

	@Override
	public Workflow updateWorkflow(UUID userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setOwnerId(userId);
		return workflowRepository.save(dbWorkflow).toModelWorkflow();
	}

	@Override
	public List<Workflow> listWorkflows(UUID userId) {
		return workflowRepository.findAllByOwnerIdOrSharedTrue(userId).stream()
				.map(workflow -> workflow.toModelWorkflow()).toList();
	}

	@Override
	public Workflow getWorkflow(UUID userId, UUID workflowId) {
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
	public void deleteWorkflow(UUID userId, UUID workflowId) {
		workflowRepository.deleteById(workflowId);
	}

	@Override
	public boolean isAllowedToExecute(UUID workflowId, UUID userId) {
		return this.getWorkflow(userId, workflowId) != null;
	}

	@Override
	public boolean isWorkflowOwned(UUID workflowId, UUID userId) {
		Workflow workflow = getWorkflow(userId, workflowId);
		if (workflow == null) {
			return false;
		}
		return workflow.getOwnerId().equals(userId);
	}

}
