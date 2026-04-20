package tom.assistant.service.agent;

import java.util.List;

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

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private AssistantQueryService assistantQueryService;
	private final AgentRegistry agentRegistry;
	private final AgentPlanner planner;

	public AgentOrchestratorServiceImpl(AgentRegistry agentRegistry, AgentPlanner planner) {
		this.agentRegistry = agentRegistry;
		this.planner = planner;
	}

	public void setAssistantQueryService(AssistantQueryService assistantQueryService) {
		this.assistantQueryService = assistantQueryService;
		planner.setAssistantQueryService(assistantQueryService);
	}

	public void execute(UserId userId, AssistantQuery query, StreamResult sr) {

		emit(sr, "Planning steps...");

		List<AgentStep> steps = planner.plan(userId, query);

		if (steps == null || steps.isEmpty()) {
			emit(sr, "No plan generated. Falling back.");

			AgentStep fallback = new AgentStep();
			fallback.setWorker("general");
			fallback.setName("fallback");

			assistantQueryService.runSingleLlmCallStreaming(userId,
					agentRegistry.getAgent("general", query, null).query(), sr);
			return;
		}

		StringBuilder plan = new StringBuilder();
		plan.append("Running plan (" + steps.size() + " steps):\n");
		for (int i = 0; i < steps.size(); i++) {
			plan.append((i + 1) + ". ").append(steps.get(i).getName()).append("(" + steps.get(i).getWorker() + ")\n");
		}
		emit(sr, plan.toString());

		PlanState planState = new PlanState(steps);
		planState.start();

		while (!planState.isDone()) {

			AgentStep currentStep = planState.currentStep().left();
			emit(sr, "Running step: " + currentStep.getName());

			try {
				StepResult result = runStep(userId, query, planState, sr);

				if (!handleResult(userId, query, planState, result, sr)) {
					return;
				}

			} catch (Exception e) {
				emit(sr, "Step failed: " + currentStep.getName());
				sr.addChunk("\nError: " + e.getMessage() + "\n\n");
				break;
			}

			planState.advanceStep();
		}

	}

	private StepResult runStep(UserId userId, AssistantQuery query, PlanState state, StreamResult sr) {
		AgentStep step = state.currentStep().left();

		return switch (step.getType()) {

		case ACTION -> runAction(userId, query, step, state, sr);

		case ASK -> runAsk(step);

		case PLAN -> runReplan(userId, query, step, state);

		default -> throw new IllegalStateException("Unknown step type: " + step.getType());
		};
	}

	private StepResult runAction(UserId userId, AssistantQuery query, AgentStep step, PlanState state,
			StreamResult sr) {
		AgentQuery agentQuery = agentRegistry.getAgent(step.getWorker(), query, state);

		String raw;
		if (step.getVisibility() == AgentResponseVisibility.USER) {
			raw = assistantQueryService.runSingleLlmCallStreaming(userId, agentQuery.query(), sr);
			sr.addChunk("\n\n");
		} else {
			raw = assistantQueryService.runSingleLlmCall(userId, agentQuery.query());
		}

		LlmParseResult parsed = LlmResponse.parse(raw);

		if (parsed.isStructured()) {

			LlmResponse response = parsed.getStructured();

			return switch (response.getStatus()) {
			case SUCCESS -> StepResult.success(response);
			case NEED_INFO -> StepResult.ask(response);
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

	private StepResult runReplan(UserId userId, AssistantQuery query, AgentStep step, PlanState state) {
		// Re-run planner with current context
		List<AgentStep> newSteps = planner.plan(userId, query, state);

		if (newSteps == null || newSteps.isEmpty()) {
			return StepResult.error("Replanning produced empty plan");
		}

		// Replace remaining steps
		state.replaceRemaining(newSteps);

		// Optional: advance immediately to next step
		state.advanceStep();

		LlmResponse response = new LlmResponse();
		response.setStatus(LlmStatus.SUCCESS);
		response.setMessage("Replanned");

		return StepResult.success(response);
	}

	private boolean handleResult(UserId userId, AssistantQuery query, PlanState state, StepResult result,
			StreamResult sr) {

		AgentStepState stepState = state.currentStep().right();

		switch (result.getType()) {

		case SUCCESS -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.SUCCESS);
			stepState.setResponse(result.getResponse());
			return true;
		}

		case ASK -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.NEED_INFO);
			stepState.setResponse(result.getResponse());

			sr.addChunk("\n" + result.getResponse().getMessage());
			sr.markComplete();

			return false;
		}

		case ERROR -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.ERROR);
			stepState.setResponse(result.getResponse());

			sr.addChunk("\nError: " + result.getResponse().getMessage());
			sr.markComplete();

			return false;
		}

		case UNSTRUCTURED -> {
			stepState.setResponse(result.getResponse());
			stepState.setStatus(LlmStatus.SUCCESS);
			stepState.setResponse(result.getResponse());
			if (state.currentStep().left().getVisibility() == AgentResponseVisibility.INTERNAL) {
				sr.addChunk("\n\n[INTERNAL STEP RESULT]" + result.getFallbackText());
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