package tom.assistant.service.agent;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

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
import tom.assistant.service.agent.model.PlanState;
import tom.assistant.service.agent.model.StepResult;
import tom.llm.service.LlmService;

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private static final Logger logger = LogManager.getLogger(AgentOrchestratorServiceImpl.class);

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

		UserMessage userMessage = UserMessage.builder().text(query.getQuery()).build();
		chatMemory.add(query.getConversationId().getValue().toString(), userMessage);

		StringBuilder assistantMessageBuilder = new StringBuilder();

		emit(sr, "Planning steps...");

		List<AgentStep> steps = null;
		try {
			steps = planner.plan(userId, query);
		} catch (Exception e) {
			logger.error("Planner failed to create a plan: " + e.toString());
		}

		if (steps == null || steps.isEmpty()) {
			emit(sr, "No plan generated. Falling back.");

			AgentStep fallback = new AgentStep();
			fallback.setWorker("general");
			fallback.setName("fallback");

			assistantQueryService.runSingleLlmCallStreaming(userId,
					agentRegistry.getAgent("general", query, null).query(), sr);
			return;
		}

		emit(sr, "Running plan (" + steps.size() + " steps):\n");
		for (int i = 0; i < steps.size(); i++) {
			emit(sr, "" + (i + 1) + ". " + steps.get(i).getName() + "(" + steps.get(i).getWorker() + ") - "
					+ steps.get(i).getVisibility().toString());
		}

		PlanState planState = new PlanState(steps);
		planState.start();

		while (!planState.isDone()) {

			AgentStep currentStep = planState.currentStep().left();
			emit(sr, "Running step: " + currentStep.getName());

			try {
				StepResult result = runStep(userId, query, planState, sr);

				if (!handleResult(userId, query, planState, result, sr, assistantMessageBuilder)) {
					return;
				}

			} catch (Exception e) {
				emit(sr, "Step failed: " + currentStep.getName());
				sr.addChunk("\nError: " + e.getMessage() + "\n\n");
				break;
			}

			planState.advanceStep();
		}

		AssistantMessage assistantMessage = AssistantMessage.builder().content(assistantMessageBuilder.toString())
				.build();
		chatMemory.add(query.getConversationId().getValue().toString(), assistantMessage);
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
				logger.info("success branch add assistant message");
				assistantMessageBuilder.append("\n\n").append(result.getResponse().getMessage());
			}
			return true;
		}

		case ASK -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.NEED_INFO);

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
				logger.info("unstructured branch add assistant message");
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