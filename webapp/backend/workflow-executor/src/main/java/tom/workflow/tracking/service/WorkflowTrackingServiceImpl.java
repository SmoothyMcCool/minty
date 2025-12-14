package tom.workflow.tracking.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.UserId;
import tom.api.MintyProperties;
import tom.workflow.executor.WorkflowRunner;
import tom.workflow.tracking.controller.model.WorkflowResult;
import tom.workflow.tracking.controller.model.WorkflowState;
import tom.workflow.tracking.model.WorkflowExecution;
import tom.workflow.tracking.repository.WorkflowExecutionRepository;

@Service
public class WorkflowTrackingServiceImpl implements WorkflowTrackingService {

	private final Logger logger = LogManager.getLogger(WorkflowTrackingServiceImpl.class);

	private final WorkflowExecutionRepository workflowExecutionRepository;
	private final List<WorkflowRunner> runningWorkflows;
	private final String logFileDirectory;

	public WorkflowTrackingServiceImpl(WorkflowExecutionRepository workflowExecutionRepository,
			MintyProperties properties) {
		this.workflowExecutionRepository = workflowExecutionRepository;
		runningWorkflows = new ArrayList<>();
		logFileDirectory = properties.get("workflowLogs");
		if (logFileDirectory == null) {
			throw new RuntimeException("workflowLogs property is not defined.");
		}
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
	public synchronized List<WorkflowState> getWorkflowList(UserId userId) {
		List<WorkflowState> memoryResults = runningWorkflows.stream().filter(item -> item.getUser().equals(userId))
				.map(item -> new WorkflowState(item.getExecutionState().getId(), item.getWorkflowName(),
						item.getExecutionState().getState()))
				.toList();
		List<WorkflowState> dbResults = workflowExecutionRepository.findAllByOwnerId(userId).stream()
				.map(item -> new WorkflowState(item.getId(), item.getName(), item.getState())).toList();
		return Stream.concat(memoryResults.stream(), dbResults.stream()).toList();
	}

	@Override
	@Transactional
	public WorkflowResult getResult(UserId userId, UUID resultId) {
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
		return new WorkflowResult(we.getId(), we.getName(), we.getExecutionRecord().getResult(),
				we.getExecutionRecord().getOutput(), we.getExecutionRecord().getOutputFormat());
	}

	@Override
	public String getOutput(UserId userId, UUID resultId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(resultId);

		if (execution.isEmpty()) {
			logger.warn("Workflow ID " + resultId + " does not exist.");
			return "";
		}

		if (!execution.get().getOwnerId().equals(userId)) {
			logger.warn("Execution " + resultId + " is not owned by " + userId);
			return "";
		}

		return execution.get().getExecutionRecord().getOutput();

	}

	@Override
	@Transactional
	public String getLog(UserId userId, UUID resultId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(resultId);

		if (execution.isEmpty()) {
			logger.warn("Workflow ID " + resultId + " does not exist.");
			return "";
		}

		if (!execution.get().getOwnerId().equals(userId)) {
			logger.warn("Execution " + resultId + " is not owned by " + userId);
			return "";
		}

		Path path = Path.of(logFileDirectory + "/" + execution.get().getResult().getLogFile());
		String content;
		try {
			content = Files.readString(path);
		} catch (IOException e) {
			content = "Log file not found.";
		}
		return content;
	}

	@Override
	@Transactional
	public void deleteResult(UserId userId, UUID workflowId) {
		Optional<WorkflowExecution> execution = workflowExecutionRepository.findById(workflowId);

		if (execution.isPresent() && execution.get().getOwnerId().equals(userId)) {

			Path logFile = null;
			try {
				logFile = Path.of(logFileDirectory + "/" + execution.get().getResult().getLogFile());
				Files.delete(logFile);
			} catch (Exception e) {
				logger.warn("While deleting result " + workflowId.toString() + ", failed to delete associated log file "
						+ logFile != null ? logFile : "<<Failed to construct file path>>");
			}

			workflowExecutionRepository.delete(execution.get());
		}
	}

}
