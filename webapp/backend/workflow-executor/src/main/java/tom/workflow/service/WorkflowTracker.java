package tom.workflow.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.task.AsyncTaskExecutor;

import tom.assistant.service.management.AssistantManagementServiceInternal;
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
		workflowConversation = conversationService.newConversation(userId,
				AssistantManagementServiceInternal.WorkflowDefaultAssistantId);
	}

	public synchronized void workflowComplete() {
		// Ensure we only generate output once.
		if (taskComplete) {
			return;
		}

		taskComplete = true;

		results.stop();

		logger.info("Workflow " + getWorkflowName() + " is complete.");

		try {
			// Get all conversations used in this task and append them to results, then
			// delete them.
			List<ChatMessage> chats = conversationService.getChatMessages(userId,
					workflowConversation.getConversationId());
			if (!chats.isEmpty()) {
				results.setChatMessages(chats);
			}

			TaskRequest outputTaskRequest = new TaskRequest();
			outputTaskRequest.setName(workflow.getOutputStep().getName());
			outputTaskRequest.setConfiguration(workflow.getOutputStep().getConfiguration());
			OutputTask outputTask = taskRegistryService.newOutputTask(userId, outputTaskRequest);

			outputTask.execute(results);

		} catch (IOException e) {
			logger.error("Failed to generate output for " + workflow.getName() + " with exception ", e);
		} finally {
			conversationService.deleteConversation(userId, workflowConversation.getConversationId());
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
			return;
		}

		Task step = workflow.getWorkflowSteps().get(0);
		TaskRequest taskRequest = new TaskRequest(step.getName(), step.getConfiguration());
		AiTask currentTask = taskRegistryService.newTask(userId, taskRequest);

		// Start the processing chain with a new conversation. Every task gets the
		// conversation ID. They have to be careful in how its used. Best not use it in
		// parallelized tasks...
		Map<String, Object> input = new HashMap<>();
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

		logger.info("completed task " + completedTask.getTaskId() + " for step " + completedTask.getStepNumber());
		// In the situation where no task generates output but there are more steps, we
		// have to wait for all tasks to complete as we do not want to manually trigger
		// a step if there are preceding inputs (in this situation, the follow-on step
		// should only trigger once).
		List<Map<String, Object>> output = completedTask.getOutput();
		int currentWorkflowStep = completedTask.getStepNumber();
		int lastWorkflowStep = workflow.getWorkflowSteps().size() - 1;

		boolean manuallyTriggerNextStep = false;

		if (pendingTasks.isEmpty()) {
			// If there are no more pending tasks and the current step is the last step, we
			// are totally done.
			if (currentWorkflowStep == lastWorkflowStep) {
				workflowComplete();
				return;
			} else {
				if (completedTask.getOutput().size() == 0) {
					// If we got here, then we are in the situation where all currently running
					// tasks are done, and none of them produced an output to trigger the next step,
					// so we have to ensure it triggers.
					manuallyTriggerNextStep = true;
					// There is output from this task, let use the output to generate further tasks.
					// Nothing to do. Keep running.
				} // else { // Fake else :)
					// There is output from this task, let use the output to generate further tasks.
					// Nothing to do. Keep running.
					// }
			}
		} else {
			// If there are pending tasks but no output from this task, then do nothing.
			// Just let other tasks keep running.
			if (output.isEmpty()) {
				return;
			}

			// If there are no pending tasks and output is empty, then we run a single
			// instance of the follow-on step, unless this task was part of the last step.
			if (currentWorkflowStep == lastWorkflowStep) {
				return;
			}
		}

		int nextStep = ++currentWorkflowStep;
		Task step = workflow.getWorkflowSteps().get(nextStep);
		TaskRequest taskRequest = new TaskRequest(step.getName(), step.getConfiguration());

		if (manuallyTriggerNextStep) {
			WorkflowTaskWrapper wrappedTask = createTask(nextStep, taskRequest, null);
			pendingTasks.put(stepTaskCount, wrappedTask);
			logger.info("starting manual task " + wrappedTask.getTaskId() + " for step " + wrappedTask.getStepNumber());
			taskExecutor.execute(wrappedTask);
		} else {
			// An list of outputs creates one task per list item.
			for (Map<String, Object> prevOut : completedTask.getOutput()) {
				WorkflowTaskWrapper wrappedTask = createTask(nextStep, taskRequest, prevOut);
				pendingTasks.put(stepTaskCount, wrappedTask);
				logger.info(
						"starting auto task " + wrappedTask.getTaskId() + " for step " + wrappedTask.getStepNumber());
				taskExecutor.execute(wrappedTask);
			}
		}

	}

	private WorkflowTaskWrapper createTask(int nextStep, TaskRequest taskRequest, Map<String, Object> prevOut) {
		AiTask task = taskRegistryService.newTask(userId, taskRequest);

		Map<String, Object> taskInput = prevOut;

		if (prevOut != null) {
			taskInput = new HashMap<>(prevOut);
		} else {
			taskInput = new HashMap<>();
		}
		taskInput.put("Conversation ID", workflowConversation.getConversationId());

		task.setInput(taskInput);

		WorkflowTaskWrapper wrapper = new WorkflowTaskWrapper(++stepTaskCount, nextStep, task, this, taskRequest);
		return wrapper;
	}

}
