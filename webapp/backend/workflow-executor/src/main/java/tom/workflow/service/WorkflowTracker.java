package tom.workflow.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.conversation.model.Conversation;
import tom.conversation.service.ConversationServiceInternal;
import tom.model.ChatMessage;
import tom.output.ExecutionResult;
import tom.output.OutputTask;
import tom.task.AiTask;
import tom.task.taskregistry.TaskRegistryService;
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
	private Conversation workflowConversation;

	public WorkflowTracker(int userId, Workflow workflow, TaskRegistryService taskRegistryService,
			ConversationServiceInternal conversationService, AsyncTaskExecutor taskExecutor) {
		taskComplete = false;
		this.userId = userId;
		this.workflow = workflow;
		this.taskRegistryService = taskRegistryService;
		this.conversationService = conversationService;
		this.taskExecutor = taskExecutor;
		results = new ExecutionResult(workflow.numSteps());
		workflowConversation = conversationService.newConversation(userId, -1); // Assistant ID doesn't matter.
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
		List<ChatMessage> chats = conversationService.getChatMessages(userId, workflowConversation.getConversationId());
		if (!chats.isEmpty()) {
			results.setChatMessages(chats);
		}
		conversationService.deleteConversation(userId, workflowConversation.getConversationId());

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

		// Start the processing chain with a new conversation. Every task gets the
		// conversation ID. They have to be careful in how its used. Best not use it in
		// parallelized tasks...
		Map<String, String> input = new HashMap<>();
		input.put("Conversation ID", workflowConversation.getConversationId());
		currentTask.setInput(input);

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

			taskInput = new HashMap<>(prevOut);
			taskInput.put("Conversation ID", workflowConversation.getConversationId());

			task.setInput(taskInput);

			WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(++stepTaskCount, nextStep, task, this, taskRequest);
			pendingTasks.put(stepTaskCount, wrapper);
			taskExecutor.execute(wrapper);
		}

	}

}
