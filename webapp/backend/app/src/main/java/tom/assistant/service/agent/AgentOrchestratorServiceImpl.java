package tom.assistant.service.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.llm.LlmParseResult;
import tom.assistant.service.agent.llm.LlmResponse;
import tom.assistant.service.agent.llm.LlmStatus;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentResponseVisibility;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;
import tom.assistant.service.agent.model.AgentStepType;
import tom.assistant.service.agent.model.PlanState;
import tom.assistant.service.agent.model.StepResult;
import tom.llm.service.LlmService;

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private static final ObjectMapper Mapper = new ObjectMapper();
	private static final Logger logger = LogManager.getLogger(AgentOrchestratorServiceImpl.class);

	private static final String PlanStateMarker = "PLAN_STATE::";

	private AssistantQueryService assistantQueryService;
	private final AgentRegistryImpl agentRegistry;
	private final AgentPlanner planner;
	private final ChatMemory chatMemory;

	public AgentOrchestratorServiceImpl(AgentRegistryImpl agentRegistry, AgentPlanner planner, LlmService llmService) {
		this.agentRegistry = agentRegistry;
		this.planner = planner;
		this.chatMemory = llmService.getChatMemory();
	}

	public void setAssistantQueryService(AssistantQueryService assistantQueryService) {
		this.assistantQueryService = assistantQueryService;
		planner.setAssistantQueryService(assistantQueryService);
	}

	public void execute(UserId userId, AssistantQuery query, StreamResult sr) {

		PlanState planState = null;

		String conversationId = query.getConversationId().getValue().toString();

		planState = recoverPlan(sr, conversationId, query);

		if (planState == null) {
			planState = createPlan(sr, userId, query);
			planState.start();
		}

		UserMessage userMessage = UserMessage.builder().text(query.getQuery()).build();
		chatMemory.add(conversationId, userMessage);

		StringBuilder assistantMessageBuilder = new StringBuilder();

		boolean stopEarly = false;
		while (!planState.isDone() && !planState.isErrored() && !stopEarly) {

			AgentStep currentStep = planState.currentStep().left();
			emit(sr, "Running step: " + currentStep.getName());

			try {
				StepResult result = runStep(userId, query, planState, sr);

				if (!handleResult(userId, query, planState, result, sr, assistantMessageBuilder)) {
					stopEarly = true;
				}

			} catch (Exception e) {
				emit(sr, "Step failed: " + currentStep.getName());
				sr.addChunk("\nError: " + e.getMessage() + "\n\n");
				break;
			}

			// This must be guarded because we only stop early on errors or questions, both
			// of which rely on the current state not changing.
			if (!stopEarly) {
				planState.advanceStep();
			}
		}

		AssistantMessage assistantMessage = AssistantMessage.builder().content(assistantMessageBuilder.toString())
				.build();
		chatMemory.add(conversationId, assistantMessage);

		if (!planState.isDone() && !planState.isErrored()) { // isDone checks if the plan is complete or the current
																// step errored.
			try {
				SystemMessage planCache = SystemMessage.builder()
						.text(PlanStateMarker + Mapper.writeValueAsString(planState)).build();
				chatMemory.add(conversationId, planCache);
			} catch (JsonProcessingException e) {
				logger.error("Failed to persist plan state.", e);
			}
		}
	}

	private PlanState recoverPlan(StreamResult sr, String conversationId, AssistantQuery query) {
		PlanState planState = null;
		List<Message> messages = chatMemory.get(conversationId);

		if (messages != null && !messages.isEmpty()) {

			// Is the last message a system message?
			Message lastMessage = messages.getLast();
			if (lastMessage.getMessageType() == MessageType.SYSTEM && messages.size() > 1) {
				// Try to decode a state object.
				try {
					String planAsString = lastMessage.getText().substring(PlanStateMarker.length());
					planState = Mapper.readValue(planAsString, PlanState.class);

					List<AgentStep> steps = planState.getSteps().stream().map(step -> step.left()).toList();

					// If the current step is a question, advance the step and add the users
					// response to the next step.
					int currentStep = planState.getCurrentStepIndex();
					if (planState.getSteps().get(currentStep).left().getType() == AgentStepType.ASK) {
						planState.advanceStep();
					}

					emit(sr, "Resuming plan at step " + (currentStep + 1) + " of " + steps.size() + "\n");
					for (int i = planState.getCurrentStepIndex(); i < steps.size(); i++) {
						emit(sr, "" + (i + 1) + ". " + steps.get(i).getName() + "(" + steps.get(i).getWorker() + ") - "
								+ steps.get(i).getVisibility().toString());
					}

					// Add the user's response message into the plan step, since agents don't get
					// the chat history.
					Map<String, Object> stepInput = planState.currentStep().left().getInput();
					stepInput.put("User Response", query.getQuery());

				} catch (JsonProcessingException e) {
					// Not a valid plan object.
					planState = null;
				}
				// If the last message was a system message and there was more than one message,
				// then plan or not it was an attempt at storing a plan, so lets remove it.
				messages = new ArrayList<>(messages); // Clone it to guard against an API giving us a read-only list
				messages.removeLast();
				chatMemory.clear(conversationId);
				chatMemory.add(conversationId, messages);
			}
		}

		return planState;
	}

	private PlanState createPlan(StreamResult sr, UserId userId, AssistantQuery query) {
		emit(sr, "Planning steps...");

		List<AgentStep> steps = null;

		try {
			steps = planner.plan(userId, query);
		} catch (Exception e) {
			logger.error("Planner failed to create a plan: " + e.toString());
		}

		if (steps == null || steps.isEmpty()) {
			emit(sr, "No plan generated. Falling back.");

			steps = new ArrayList<>();

			AgentStep fallback = new AgentStep();
			fallback.setWorker("general");
			fallback.setName("fallback");
			fallback.setType(AgentStepType.ACTION);
			fallback.setVisibility(AgentResponseVisibility.USER);
			fallback.setInput(Map.of("query", query.getQuery()));
			fallback.setId("general query");

			steps.add(fallback);
			return new PlanState(steps);
		}

		emit(sr, "Running plan (" + steps.size() + " steps):\n");
		for (int i = 0; i < steps.size(); i++) {
			emit(sr, "" + (i + 1) + ". " + steps.get(i).getName() + "(" + steps.get(i).getWorker() + ") - "
					+ steps.get(i).getVisibility().toString());
		}

		return new PlanState(steps);

	}

	private StepResult runStep(UserId userId, AssistantQuery query, PlanState state, StreamResult sr) {
		AgentStep step = state.currentStep().left();

		return switch (step.getType()) {

		case ACTION -> runAction(userId, query, step, state, sr);

		case ASK -> runAsk(step);

		case PLAN -> runReplan(userId, query, step, state, sr);

		default -> throw new IllegalStateException("Unknown step type: " + step.getType());
		};
	}

	private StepResult runAction(UserId userId, AssistantQuery query, AgentStep step, PlanState state,
			StreamResult sr) {
		AgentQuery agentQuery = agentRegistry.getAgent(step.getWorker(), query, state);

		String raw;
		sr.addChunk("[INTERNAL][" + state.currentStep().left().getName() + "]Query: " + agentQuery.query().getQuery());
		if (step.getVisibility() == AgentResponseVisibility.USER) {
			raw = assistantQueryService.runSingleLlmCallStreaming(userId, agentQuery.query(), sr);
			sr.addChunk("<br><br>");
		} else {
			raw = assistantQueryService.runSingleLlmCall(userId, agentQuery.query());
		}

		LlmParseResult parsed = LlmResponse.parse(raw);

		if (parsed.isStructured()) {

			LlmResponse response = parsed.getStructured();

			return switch (response.getStatus()) {
			case SUCCESS -> StepResult.success(response);
			case NEED_INFO -> StepResult.ask(response);
			case REPLAN -> StepResult.replan(response);
			case ERROR -> StepResult.error(response);
			case PENDING -> throw new UnsupportedOperationException("Unimplemented case: " + response.getStatus());
			default -> throw new IllegalArgumentException("Unexpected value: " + response.getStatus());
			};
		}

		return StepResult.unstructured(parsed.getFallbackText());
	}

	private StepResult runAsk(AgentStep step) {
		String question = (String) step.getInput().get("question");
		return StepResult.ask(question);
	}

	private StepResult runReplan(UserId userId, AssistantQuery query, AgentStep step, PlanState state,
			StreamResult sr) {
		// Re-run planner with current context
		List<AgentStep> newSteps = planner.plan(userId, query, state);

		if (newSteps == null || newSteps.isEmpty()) {
			return StepResult.error("Replanning produced empty plan");
		}

		// Replace remaining steps
		state.replaceRemaining(newSteps);

		LlmResponse response = new LlmResponse();
		response.setStatus(LlmStatus.SUCCESS);
		response.setMessage("Replanned");

		emit(sr, "Replanned. New steps:");
		for (int i = 0; i < state.getSteps().size(); i++) {
			emit(sr, "" + (i + 1) + ". " + state.getSteps().get(i).left().getName() + "("
					+ state.getSteps().get(i).left().getWorker() + ") - "
					+ state.getSteps().get(i).left().getVisibility().toString());
		}

		return StepResult.success(response);
	}

	private boolean handleResult(UserId userId, AssistantQuery query, PlanState state, StepResult result,
			StreamResult sr, StringBuilder assistantMessageBuilder) {

		AgentStepState stepState = state.currentStep().right();

		switch (result.getType()) {

		case SUCCESS -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.SUCCESS);
			if (state.currentStep().left().getVisibility() == AgentResponseVisibility.INTERNAL) {
				sr.addChunk("[INTERNAL][" + state.currentStep().left().getName() + "]" + result.getResponse().toString()
						+ "<br><br>");
			} else {
				// User-facing message. Add it to chat history.
				assistantMessageBuilder.append("\n\n").append(result.getResponse().getMessage());
			}
			return true;
		}

		case ASK -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.NEED_INFO);

			assistantMessageBuilder.append("\n\n").append(result.getResponse().getMessage());
			sr.addChunk("<br>" + result.getResponse().getMessage());
			sr.markComplete();

			return false;
		}

		case REPLAN -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.REPLAN);
			// Insert a replan step into the plan.
			state.addReplanStep();
			sr.addChunk("<br>Replan was requested.");
			sr.addChunk("<br>" + result.getResponse().getMessage());
			return true;
		}
		case ERROR -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.ERROR);
			state.setErrored(true);

			sr.addChunk("<br>Error: " + result.getResponse().getMessage());
			sr.markComplete();

			return false;
		}

		case UNSTRUCTURED -> {
			stepState.setUnstructuredResponse(result.getFallbackText());
			stepState.setStatus(LlmStatus.SUCCESS);
			if (state.currentStep().left().getVisibility() == AgentResponseVisibility.INTERNAL) {
				sr.addChunk("[INTERNAL][" + state.currentStep().left().getName() + "]" + result.getFallbackText());
			} else {
				// User-facing message. Add it to chat history.
				assistantMessageBuilder.append("\n\n").append(result.getFallbackText());
			}
			return true;
		}
		}

		return false;
	}

	private void emit(StreamResult sr, String msg) {
		sr.addChunk("[STATUS] " + msg);
	}
}