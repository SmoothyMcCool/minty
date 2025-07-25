package tom.workflow.service;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import tom.task.model.Task;
import tom.task.taskregistry.TaskRegistryService;
import tom.workflow.controller.WorkflowRequest;
import tom.workflow.model.Workflow;
import tom.workflow.repository.WorkflowRepository;

@Service
public class WorkflowServiceImpl implements WorkflowService {

	private final Logger logger = LogManager.getLogger(WorkflowServiceImpl.class);

	private final WorkflowRepository workflowRepository;
	private final TaskRegistryService taskRegistryService;
	private final AsyncTaskExecutor taskExecutor;

	public WorkflowServiceImpl(WorkflowRepository workflowRepository, TaskRegistryService taskRegistryService,
			@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
		this.workflowRepository = workflowRepository;
		this.taskRegistryService = taskRegistryService;
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void executeWorkflow(int userId, WorkflowRequest request) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(request.getId());
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + request.getId() + ". Cannot run.");
			return;
		}

		Workflow workflow = maybeWorkflow.get().toModelWorkflow();
		List<Task> steps = workflow.getWorkflowSteps();

		if (steps.size() != request.getTaskConfigurationList().size()) {
			logger.warn("Expected to get " + steps.size() + " configuration objects, but instead got "
					+ request.getTaskConfigurationList().size());
			return;
		}

		for (int i = 0; i < steps.size(); i++) {
			steps.get(i).setConfiguration(request.getTaskConfigurationList().get(i));
		}
		workflow.getOutputStep().setConfiguration(request.getOutputConfiguration());

		WorkflowTracker tracker = new WorkflowTracker(userId, workflow, taskRegistryService, taskExecutor);
		tracker.runFirstTask();
	}

	@Override
	public Workflow createWorkflow(int userId, Workflow workflow) {
		tom.workflow.repository.Workflow dbWorkflow = new tom.workflow.repository.Workflow(workflow);
		dbWorkflow.setId(null);
		return workflowRepository.save(dbWorkflow).toModelWorkflow();
	}

	@Override
	public List<Workflow> listWorkflows(int userId) {
		return workflowRepository.findAllByOwnerIdOrSharedTrue(userId).stream()
				.map(workflow -> workflow.toModelWorkflow()).toList();
	}

	@Override
	public Workflow getWorkflow(int userId, int workflowId) {
		Optional<tom.workflow.repository.Workflow> maybeWorkflow = workflowRepository.findById(workflowId);
		if (maybeWorkflow.isEmpty()) {
			logger.warn("Did not find workflow for ID " + workflowId);
			return null;
		}

		return maybeWorkflow.get().toModelWorkflow();
	}

	@Override
	public void deleteWorkflow(int userId, int workflowId) {
		workflowRepository.deleteById(workflowId);
	}

}
