package tom.workflow.executor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.api.UserId;
import tom.task.OutputTask;
import tom.task.TaskSpec;
import tom.util.StackTraceUtilities;
import tom.workflow.model.Connection;
import tom.workflow.model.TaskRequest;
import tom.workflow.model.Workflow;
import tom.workflow.taskregistry.TaskRegistryService;
import tom.workflow.tracking.model.WorkflowExecution;
import tom.workflow.tracking.service.WorkflowTrackingService;

public class WorkflowRunner {

	private static final ExecutorService Executor = Executors.newVirtualThreadPerTaskExecutor();

	private final TaskRegistryService taskRegistryService;
	private final WorkflowTrackingService workflowTrackingService;
	private final AsyncTaskExecutor taskExecutor;
	private final Workflow workflow;
	private boolean workflowComplete;
	private UserId userId;
	private WorkflowExecution executionState;
	private WorkflowLoggerImpl logger;
	private String logFolder;

	public WorkflowRunner(UserId userId, Workflow workflow, TaskRegistryService taskRegistryService,
			WorkflowTrackingService workflowTrackingService, AsyncTaskExecutor taskExecutor,
			String workflowLoggingFolder) {
		workflowComplete = false;
		this.userId = userId;
		this.workflow = workflow;
		this.taskRegistryService = taskRegistryService;
		this.workflowTrackingService = workflowTrackingService;
		this.taskExecutor = taskExecutor;
		this.executionState = null;
		logFolder = workflowLoggingFolder;
	}

	public WorkflowExecution getExecutionState() {
		return executionState;
	}

	public synchronized void workflowComplete() {
		// Ensure we only generate output once.
		if (workflowComplete) {
			return;
		}

		workflowComplete = true;

		executionState.getResult().stop();

		logger.info("Workflow " + getWorkflowName() + " is complete.");

		try {
			if (workflow.getOutputStep() != null) {
				TaskRequest outputTaskRequest = new TaskRequest();
				outputTaskRequest.setTaskName(workflow.getOutputStep().getTaskName());
				outputTaskRequest.setConfiguration(workflow.getOutputStep().getConfiguration());
				OutputTask outputTask = taskRegistryService.newOutputTask(userId, outputTaskRequest);

				executionState.setOutput(outputTask.execute(executionState.getResult().toApiResult()));
				executionState.setOutputFormat(outputTask.getSpecification().getFormat());
			} else {
				logger.warn("This workflow has no output task set. Cannot produce any output.");
			}

		} catch (Exception e) {
			executionState.setOutput(StackTraceUtilities.StackTrace(e));
			executionState.setOutputFormat("text/plain");

			logger.error("Failed to generate output for " + workflow.getName() + " with exception ", e);
		} finally {
			executionState.setName(getWorkflowName());
			workflowTrackingService.workflowCompleted(this);
			logger.close();
		}
	}

	public String getWorkflowName() {
		return workflow.getName() + " - " + executionState.getResult().getStartTime().toString();
	}

	public void start() {

		List<CompletableFuture<Void>> tasks = new ArrayList<>();

		executionState = new WorkflowExecution();
		executionState.setOwnerId(userId);
		executionState.getResult().start();

		String logFile = getWorkflowName() + ".log";
		logger = new WorkflowLoggerImpl(logFolder, logFile);
		logFile = logger.getFileName();
		executionState.getResult().setLogFile(logFile);

		try {
			// Instantiate all our connectors;
			List<ImmutablePair<Connection, Connector>> connectors = new ArrayList<>();
			for (int i = 0; i < workflow.getConnections().size(); i++) {
				connectors.add(ImmutablePair.of(workflow.getConnections().get(i), new Connector()));
			}

			for (TaskRequest request : workflow.getSteps()) {

				TaskRunner runner = new TaskRunner(userId, taskRegistryService, request, taskExecutor, logger);
				executionState.addStep(request.getStepName());

				// Fetch and store the state now, so we can get live updates as the workflow
				// progresses.
				executionState.getState().getStepStates().put(request.getStepName(), runner.getState());

				List<Connector> inputs = connectors.stream().filter((item) -> {
					return item.left.getReaderId().equals(request.getId());
				}).sorted((first, second) -> {
					return first.left.getReaderPort() - second.left.getReaderPort();
				}).collect(Collectors.collectingAndThen(Collectors.toList(), list -> {

					TaskSpec spec = taskRegistryService.getSpecForTask(request.getTaskName());
					if (spec == null) {
						throw new RuntimeException("Invalid task specified! " + request.getTaskName());
					}

					List<Connector> connectorList = new ArrayList<>(
							Collections.nCopies(spec.numInputs(), new NullConnector()));

					for (ImmutablePair<Connection, Connector> c : list) {
						connectorList.set(c.left.getReaderPort(), c.right);
					}

					return connectorList;
				}));
				runner.setInputConnectors(inputs);

				List<Connector> outputs = connectors.stream().filter((item) -> {
					return item.left.getWriterId().equals(request.getId());
				}).sorted((first, second) -> {
					return first.left.getWriterPort() - second.left.getWriterPort();
				}).collect(Collectors.collectingAndThen(Collectors.toList(), list -> {

					TaskSpec spec = taskRegistryService.getSpecForTask(request.getTaskName());
					if (spec == null) {
						throw new RuntimeException("Invalid task specified! " + request.getTaskName());
					}

					List<Connector> connectorList = new ArrayList<>(
							Collections.nCopies(spec.numOutputs(), new NullConnector()));

					for (ImmutablePair<Connection, Connector> c : list) {
						connectorList.set(c.left.getWriterPort(), c.right);
					}

					return connectorList;
				}));

				runner.setOutputConnectors(outputs);

				CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
					try {
						runner.run();
						executionState.getResult().getErrors().put(runner.getName(), runner.getErrors());
						executionState.getResult().getResults().put(runner.getName(), runner.getResults());

						if (runner.failed()) {
							throw new WorkflowRunnerException("Runner " + runner.getName() + " failed.");
						}

					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} catch (Exception e) {
						throw new WorkflowRunnerException("Runner " + runner.getName() + " failed.", e);
					}
					return null;
				}, Executor);

				tasks.add(future);
			}

			CompletableFuture<Void> allDone = allOfFailFast(tasks);
			allDone.whenComplete((r, ex) -> {
				if (ex != null) {
					logger.warn("A task failed with exception: ", ex);
				}
				workflowComplete();
			});
		} catch (Exception e) {
			logger.warn("Workflow failed with exception: ", e);
			logger.close();
		}

	}

	public UserId getUser() {
		return userId;
	}

	private <T> CompletableFuture<Void> allOfFailFast(Collection<? extends CompletableFuture<? extends T>> futures) {
		CompletableFuture<Void> failFast = new CompletableFuture<>();

		for (CompletableFuture<? extends T> f : futures) {
			f.whenComplete((r, ex) -> {
				if (ex != null) {
					logger.warn("Future ended with exception!", ex);
					failFast.completeExceptionally(ex);
				}
			});
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenRun(() -> failFast.complete(null));

		return failFast;
	}
}
