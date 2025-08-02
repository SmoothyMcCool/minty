package tom.workflow.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.conversation.service.ConversationServiceInternal;
import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.taskregistry.TaskRegistryService;
import tom.tasks.ai.query.AiQueryTask;
import tom.workflow.model.Task;
import tom.workflow.model.TaskRequest;
import tom.workflow.model.Workflow;

public class WorkflowTracker {

	private final Logger logger = LogManager.getLogger(WorkflowTracker.class);

	private final TaskRegistryService taskRegistryService;
	private final ConversationServiceInternal conversationService;
	private final AsyncTaskExecutor taskExecutor;
	private final Workflow workflow;
	private boolean taskComplete;
	private int userId;
	private UUID uuid = UUID.randomUUID();
	private ExecutionResult results;
	private int stepTaskCount = 0;
	private Map<Integer, WorkflowTaskWrapper> pendingTasks = new HashMap<>();
	private String workflowConversationId;

	public WorkflowTracker(int userId, Workflow workflow, TaskRegistryService taskRegistryService,
			ConversationServiceInternal conversationService, AsyncTaskExecutor taskExecutor) {
		taskComplete = false;
		this.userId = userId;
		this.workflow = workflow;
		this.taskRegistryService = taskRegistryService;
		this.conversationService = conversationService;
		this.taskExecutor = taskExecutor;
		results = new ExecutionResult(workflow.numSteps());

		workflowConversationId = conversationService.newConversationId(userId, 999, getWorkflowName());
	}

	public synchronized void workflowComplete() {
		// Ensure we only generate output once.
		if (taskComplete) {
			return;
		}

		taskComplete = true;

		results.stop();

		logger.info("Workflow " + getWorkflowName() + " is complete.");

		// Get all conversations used in this task and append them to results, then
		// delete them.
		List<List<String>> chats = conversationService.getConversationsForWorkflow(getWorkflowName());
		if (!chats.isEmpty()) {
			results.setChats(chats);
		}
		conversationService.deleteConversationsForWorkflow(userId, getWorkflowName());

		TaskRequest outputTaskRequest = new TaskRequest();
		outputTaskRequest.setName(workflow.getOutputStep().getName());
		outputTaskRequest.setConfiguration(workflow.getOutputStep().getConfiguration());
		OutputTask outputTask = taskRegistryService.newOutputTask(userId, outputTaskRequest);

		try {
			outputTask.execute(results);
		} catch (IOException e) {
			logger.error("Failed to generate output for " + workflow.getName());
		}
	}

	public String getWorkflowName() {
		return workflow.getName() + "-" + uuid;
	}

	public void runFirstTask() {
		logger.info("Starting workflow " + workflow.getName());

		results.start();
		if (workflow.getWorkflowSteps().isEmpty()) {
			// That was quick. I guess we're done.
			results.stop();
			return;
		}

		Task step = workflow.getWorkflowSteps().get(0);
		TaskRequest taskRequest = new TaskRequest(step.getName(), step.getConfiguration());
		AiTask currentTask = taskRegistryService.newTask(userId, taskRequest);

		// A little bit of a hack, but we have to check for AiQueryTasks, and if they
		// have no ConversationId set, we have to set one to ensure we can correctly
		// track and dispose of as necessary.
		if (currentTask instanceof AiQueryTask) {
			AiQueryTask aqt = (AiQueryTask) currentTask;
			if (!taskRequest.getConfiguration().containsKey("Conversation ID")) {
				// No conversation ID specified, so we create one that we can control and clean
				// up at the end of the workflow.
				// In this instance, assistant ID doesn't matter
				workflowConversationId = conversationService.newConversationId(userId, 99999, getWorkflowName());
				logger.info("Created workflow-specific conversation " + workflowConversationId);
				Map<String, String> input = new HashMap<>();
				input.put("Conversation ID", workflowConversationId);
				aqt.setInput(input);
			}
		}

		int stepNumber = 0;
		WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(++stepTaskCount, stepNumber, currentTask, this,
				taskRequest);

		pendingTasks.put(stepTaskCount, wrapper);

		taskExecutor.execute(wrapper);
	}

	public synchronized void taskComplete(WorkflowTaskWrapper completedTask) {

		results.addResult(completedTask.getStepNumber(), completedTask.getResult());

		// Remove the just-completed task from the list of pending tasks.
		pendingTasks.remove(completedTask.getTaskId());

		// If the list of pending tasks is empty, and there are no more tasks to
		// generate from the just completed task (or the task is the last one in the
		// list of steps), then the workflow should stop.
		List<Map<String, String>> output = completedTask.getOutput();
		int currentWorkflowStep = completedTask.getStepNumber();
		int lastWorkflowStep = workflow.getWorkflowSteps().size() - 1;
		if (pendingTasks.isEmpty() && (output.isEmpty() || currentWorkflowStep == lastWorkflowStep)) {
			workflowComplete();
			return;
		}

		// Are there steps after this one?
		if ((output.isEmpty() || currentWorkflowStep == lastWorkflowStep)) {
			// Out of steps. Either this is the last step or the task produced no output,
			// which means don't continue on.
			return;
		}

		int nextStep = ++currentWorkflowStep;
		Task step = workflow.getWorkflowSteps().get(nextStep);
		TaskRequest taskRequest = new TaskRequest(step.getName(), step.getConfiguration());

		for (Map<String, String> prevOut : completedTask.getOutput()) {
			AiTask task = taskRegistryService.newTask(userId, taskRequest);

			Map<String, String> taskInput = prevOut;

			// Nearly same hack as above but re-use the conversation ID we generated before.
			if (task instanceof AiQueryTask) {
				AiQueryTask aqt = (AiQueryTask) task;
				if (!taskRequest.getConfiguration().containsKey("Conversation ID")) {
					if (!prevOut.containsKey("Conversation ID")) {
						// There is no conversation ID specified anywhere. Add it to the input.
						// The input may be read-only so we have to clone it first.
						taskInput = new HashMap<>(prevOut);
						taskInput.put("Conversation ID", workflowConversationId);
					}
				}
			}

			task.setInput(taskInput);

			WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(++stepTaskCount, nextStep, task, this, taskRequest);
			pendingTasks.put(stepTaskCount, wrapper);
			taskExecutor.execute(wrapper);
		}

	}

}
