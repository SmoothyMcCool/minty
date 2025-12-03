package tom.workflow.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.core.task.AsyncTaskExecutor;

import tom.api.UserId;
import tom.task.MintyTask;
import tom.task.Packet;
import tom.task.TaskLogger;
import tom.workflow.model.TaskRequest;
import tom.workflow.taskregistry.TaskRegistryService;
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
		boolean allTasksSpawned = false;

		while (!allTasksSpawned) {

			MintyTask task = taskRegistryService.newTask(userId, request);
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
				allTasksSpawned = true; // Make sure the task only runs once.
			} else {
				for (int i = 0; i < numInputs; i++) {
					boolean inputDone = false;
					while (!inputDone) {
						try {
							Packet packet = inputs.get(i).read();

							if (packet != null) {
								if (task.wantsInput(i, packet)) {
									inputDone = task.giveInput(i, packet);
								} else {
									inputs.get(i).replace(packet);
									inputDone = true;
								}
							} else {
								task.inputTerminated(i);
								inputDone = true;
							}

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
			}

			if (task.readyToRun()) {

				executionState.addTask();
				CompletableFuture<Void> future = taskExecutor.submitCompletable(() -> {
					boolean failed = false;
					try {
						task.run();
					} catch (Exception e) {
						logger.warn("TaskRunner: Task failed due to exception: ", e);
						failed = true;
					}

					if (failed || task.failed()) {
						String error = task.getError();
						if (error != null && !error.isBlank()) {
							errors.add(task.getError());
						}
						executionState.failTask();
					} else {
						Packet result = task.getResult();
						if (result != null) {
							results.add(result);
						}
						executionState.completeTask();
					}

					return null;
				});

				activeTasks.add(future);

			} else {
				// If the task is not ready to run, then there are no longer sufficient inputs
				// to spawn more tasks. So no more task spawning. This step completes when
				// current active tasks complete.
				allTasksSpawned = true;
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
				allTasksSpawned = true;
			}
		}

		CompletableFuture<Void> allDone = CompletableFuture.allOf(activeTasks.toArray(new CompletableFuture[0]));

		allDone.thenRun(() -> {
			done = true;
			outputs.forEach(output -> output.complete());
			executionState.getCompletedTasks();
		});

		allDone.join();
	}
}
