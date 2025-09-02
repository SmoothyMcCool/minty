package tom.workflow.tracking.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.workflow.service.WorkflowRunner;
import tom.workflow.tracking.model.WorkflowExecution;
import tom.workflow.tracking.model.controller.WorkflowResult;
import tom.workflow.tracking.model.controller.WorkflowState;
import tom.workflow.tracking.repository.WorkflowExecutionRepository;

@Service
public class WorkflowTrackingServiceImpl implements WorkflowTrackingService {

	private final Logger logger = LogManager.getLogger(WorkflowTrackingServiceImpl.class);

	private final WorkflowExecutionRepository workflowExecutionRepository;
	private final List<WorkflowRunner> runningWorkflows;

	public WorkflowTrackingServiceImpl(WorkflowExecutionRepository workflowExecutionRepository) {
		this.workflowExecutionRepository = workflowExecutionRepository;
		runningWorkflows = new ArrayList<>();
	}

	@Override
	@Transactional
	public synchronized void workflowCompleted(WorkflowRunner runner) {
		workflowExecutionRepository.save(runner.getExecutionState());
		runningWorkflows.removeIf(workflow -> workflow.getWorkflowName().compareTo(runner.getWorkflowName()) == 0);
	}

	@Override
	public synchronized void trackWorkflow(WorkflowRunner runner) {
		runningWorkflows.add(runner);
	}

	@Override
	public synchronized List<WorkflowState> getWorkflowList(UUID userId) {
		List<WorkflowState> memoryResults = runningWorkflows.stream().filter(item -> item.getUser().equals(userId))
				.map(item -> new WorkflowState(item.getExecutionState().getId(), item.getWorkflowName(),
						item.getExecutionState().getState()))
				.toList();
		List<WorkflowState> dbResults = workflowExecutionRepository.findAllByOwnerId(userId).stream()
				.map(item -> new WorkflowState(item.getId(), item.getName(), item.getState())).toList();
		return Stream.concat(memoryResults.stream(), dbResults.stream()).toList();
	}

	@Override
	public WorkflowResult getResult(UUID userId, UUID resultId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(resultId);

		if (execution.isEmpty()) {
			logger.warn("Workflow ID " + resultId + " does not exist.");
			return null;
		}

		if (!execution.get().getOwnerId().equals(userId)) {
			logger.warn("Execution " + resultId + " is not owned by " + userId);
			return null;
		}

		WorkflowExecution we = execution.get();
		return new WorkflowResult(we.getId(), we.getName(), we.getResult(), we.getOutput(), we.getOutputFormat());
	}

	@Override
	public String getOutput(UUID userId, UUID resultId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(resultId);

		if (execution.isEmpty()) {
			logger.warn("Workflow ID " + resultId + " does not exist.");
			return "";
		}

		if (!execution.get().getOwnerId().equals(userId)) {
			logger.warn("Execution " + resultId + " is not owned by " + userId);
			return "";
		}

		return execution.get().getOutput();

	}

	@Override
	@Transactional
	public void deleteResult(UUID userId, UUID workflowId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(workflowId);
		if (execution.isPresent() && execution.get().getOwnerId().equals(userId)) {
			workflowExecutionRepository.delete(execution.get());
		}
	}

}
