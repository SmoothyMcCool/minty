package tom.workflow.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.api.UserId;
import tom.api.task.MintyTask;
import tom.api.task.Packet;
import tom.api.task.TaskLogger;
import tom.task.model.TaskRequest;
import tom.task.registry.TaskRegistryService;
import tom.workflow.futureutil.FutureUtils;
import tom.workflow.tracking.model.ExecutionStepState;

public class TaskRunner {

	private final AsyncTaskExecutor taskExecutor;
	private final TaskRegistryService taskRegistryService;
	private final TaskRequest request;
	private final List<CompletableFuture<Void>> activeTasks;
	private final List<Packet> results;
	private final List<String> errors;
	private final UserId userId;
	private List<Connector> inputs;
	private List<Connector> outputs;
	private boolean done = false;
	private final ExecutionStepState executionState;
	private boolean failed;
	private final TaskLogger logger;

	public TaskRunner(UserId userId, TaskRegistryService taskRegistryService, TaskRequest request,
			AsyncTaskExecutor taskExecutor, TaskLogger workflowLogger) {
		this.taskExecutor = taskExecutor;
		this.taskRegistryService = taskRegistryService;
		this.request = request;
		this.activeTasks = new ArrayList<>();
		this.results = new ArrayList<>();
		this.errors = new ArrayList<>();
		this.userId = userId;
		this.executionState = new ExecutionStepState();
		this.failed = false;
		this.logger = workflowLogger;
	}

	public String getName() {
		return request.getStepName();
	}

	public boolean isDone() {
		return done;
	}

	public boolean failed() {
		return failed;
	}

	public void setInputConnectors(List<Connector> inputs) {
		this.inputs = inputs;
	}

	public void setOutputConnectors(List<Connector> outputs) {
		this.outputs = outputs;
	}

	public ExecutionStepState getState() {
		return executionState;
	}

	public List<Map<String, Object>> getResults() {
		return results.stream().map(packet -> packet.toMap()).toList();
	}

	public List<String> getErrors() {
		return errors;
	}

	public void run() throws InterruptedException {
		AtomicBoolean allTasksSpawned = new AtomicBoolean(false);

		while (!allTasksSpawned.get()) {

			MintyTask task = taskRegistryService.newTask(userId, request);
			logger.info("New task created for " + request.getStepName());
			if (task == null) {
				failed = true;
				return;
			}

			task.setLogger(logger);
			task.setOutputConnectors(outputs);

			int numInputs = task.getSpecification().numInputs();

			// Special case. If the task has no inputs, make sure input is terminated
			// immediately.
			if (numInputs == 0) {
				task.inputTerminated(0);
				allTasksSpawned.set(true); // Make sure the task only runs once.
			} else {
				for (int i = 0; i < numInputs && !allTasksSpawned.get(); i++) {
					boolean inputDone = false;

					if (inputs.get(i).isComplete()) {
						continue;
					}

					while (!inputDone && !allTasksSpawned.get()) {
						try {
							Packet packet = inputs.get(i).read();

							if (packet != null) {
								if (packet == Connector.WRITING_COMPLETE) {
									logger.info("Task " + request.getStepName() + " input terminated on port " + i);
									task.inputTerminated(i);
									inputDone = true;
								} else {
									// Tasks only ever get new copies of packets.
									packet = new Packet(packet);
									if (task.wantsInput(i, packet)) {
										logger.info("Task " + request.getStepName() + " taking packet on port " + i);
										inputDone = task.giveInput(i, packet);
									} else {
										logger.info(
												"Task " + request.getStepName() + " didn't want packet on port " + i);
										inputs.get(i).replace(packet);
										inputDone = true;
									}
								}
							} // else {
								// If packet is null, that means the wait just timed out. We just have to break
								// every so often so we can catch situations where allTasksSpawned becomes true
								// from already running tasks, while we wait.
								// }

						} catch (Exception e) {
							logger.error("TaskRunner for " + request.getStepName() + " failed to read from input " + i
									+ ", with exception: ", e);
							failed = true;
							return;
						}
					}
					// Some tasks might not require inputs on all ports. Check if the task is ready
					// to run.
					if (task.readyToRun()) {
						// Break out of the for loop to prevent consuming unnecessary packets.
						break;
					}
				}

				if (allTasksSpawned.get()) {
					break;
				}
			}

			if (task.readyToRun()) {

				executionState.addTask();
				CompletableFuture<Void> future = taskExecutor.submitCompletable(() -> {
					boolean failed = false;
					try {
						logger.info("Task " + request.getStepName() + " about to run");
						task.run();
					} catch (Exception e) {
						logger.warn("TaskRunner: Task failed due to exception: ", e);
						failed = true;
					}

					if (failed || task.failed()) {
						String error = task.getError();
						if (StringUtils.isNotBlank(error)) {
							errors.add(task.getError());
						}
						executionState.failTask();

						if (task.terminalFailure()) {
							logger.warn("Task " + request.getStepName()
									+ " failed with a terminal error. Task is stopping.");
							throw new TerminalTaskError("Task " + request.getStepName()
									+ " failed with a terminal error. Task is stopping.");
						} else {
							logger.warn("Task " + request.getStepName()
									+ " failed with a non-terminal error. Workflow will attempt to continue.");
						}
					} else {
						logger.info("Task " + request.getStepName() + " completed");
						Packet result = task.getResult();
						if (result != null) {
							results.add(result);
						}
						executionState.completeTask();

						if (task.stepComplete()) {
							logger.info("Task " + request.getStepName() + " is terminated");
							allTasksSpawned.set(true);
						}
					}

					return null;
				});

				activeTasks.add(future);

			} else {
				// If the task is not ready to run, then there are no longer sufficient inputs
				// to spawn more tasks. So no more task spawning. This step completes when
				// current active tasks complete.
				allTasksSpawned.set(true);
			}

			// If all inputs are done, then we are also done.
			boolean allInputsComplete = true;
			for (Connector input : inputs) {
				if (!input.isComplete()) {
					allInputsComplete = false;
					break;
				}
			}
			if (allInputsComplete) {
				allTasksSpawned.set(true);
			}
		}

		CompletableFuture<Void> allDone = FutureUtils.allOfFailFast(activeTasks, logger);

		allDone.whenComplete((v, ex) -> {
			done = true;
			logger.info("Task " + request.getStepName() + " signalling to all outputs that task is complete.");
			outputs.forEach(output -> output.complete());
		});

		allDone.join();
	}
}
