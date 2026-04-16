package tom.assistant.service.agent;

import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.Pair;
import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentResponseType;
import tom.assistant.service.agent.model.AgentStep;
import tom.assistant.service.agent.model.AgentStepState;
import tom.assistant.service.agent.model.PlanState;
import tom.assistant.service.agent.response.LlmResponse;
import tom.assistant.service.agent.response.LlmStatus;
import tom.assistant.service.agent.worker.WorkerContext;
import tom.assistant.service.agent.worker.WorkerDecision;
import tom.assistant.service.agent.worker.WorkerHandler;
import tom.assistant.service.agent.worker.WorkerRegistry;

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private AssistantQueryService assistantQueryService;
	private final WorkerQueryFactoryService workerQueryFactoryService;
	private final AgentPlanner planner;
	private final WorkerRegistry workerRegistry;

	public AgentOrchestratorServiceImpl(WorkerQueryFactoryService workerQueryFactoryService, AgentPlanner planner,
			WorkerRegistry workerRegistry) {
		this.workerQueryFactoryService = workerQueryFactoryService;
		this.planner = planner;
		this.workerRegistry = workerRegistry;

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

			assistantQueryService.runSingleLlmCallStreaming(userId, workerQueryFactoryService.general(query).query(),
					sr);
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

			Pair<AgentStep, AgentStepState> currentStep = planState.currentStep();

			emit(sr, "Running step: " + currentStep.left().getName());

			try {
				WorkerDecision decision = runStep(userId, query, planState, sr);
				emit(sr, "Decision: " + decision.toString());

				boolean shouldContinue = handleDecision(userId, query, planState, decision, sr);
				if (!shouldContinue) {
					return;
				}

			} catch (Exception e) {
				emit(sr, "Step failed: " + currentStep.left().getName());
				sr.addChunk("\nError: " + e.getMessage() + "\n\n");
				break;
			}

			planState.advanceStep();
		}

		emit(sr, "Finalizing response...");
		synthesize(userId, query, planState, sr);
		emit(sr, "Complete.");
		sr.markComplete();
	}

	private WorkerDecision runStep(UserId userId, AssistantQuery originalQuery, PlanState state, StreamResult sr) {

		WorkerHandler handler = workerRegistry.get(state.currentStep().left().getWorker());
		WorkerContext context = new WorkerContext(userId, originalQuery, state, sr);

		return handler.handle(context); // returns WorkerDecision
	}

	private boolean handleDecision(UserId userId, AssistantQuery originalQuery, PlanState state,
			WorkerDecision decision, StreamResult sr) {

		WorkerDecision current = decision;

		while (true) {
			switch (current.getAction()) {

			case LLM_CALL -> {

				String type = decision.getInput().get("type").asText();

				AgentQuery agentQuery = switch (type) {
				case "general" -> workerQueryFactoryService.general(originalQuery, state);
				case "diagram_parser" -> workerQueryFactoryService.diagramParser(originalQuery, state);
				case "mermaid_generator" -> workerQueryFactoryService.mermaidGenerator(originalQuery, state);
				case "mermaid_validator" -> workerQueryFactoryService.mermaidValidator(originalQuery, state);
				case "plan_workflow" -> workerQueryFactoryService.workflowPlanner(originalQuery, state);
				default -> throw new IllegalStateException("Unknown LLM call type: " + type);
				};

				// Call LLM
				String raw = assistantQueryService.runSingleLlmCall(userId, agentQuery.query());

				// Deserialize into LlmResponse
				LlmResponse llm;
				try {
					llm = LlmResponse.parse(raw, agentQuery.responsetype());
				} catch (JsonProcessingException e) {
					emit(sr, "Agent returned an invalid response: " + raw);
					return false;
				}

				// Store structured result
				state.currentStep().right().setResponse(llm);
				state.currentStep().right().setStatus(llm.getStatus());

				if (llm.getResponseType() == AgentResponseType.Structured) {
					if (llm.getStatus() == LlmStatus.NEED_INFO) {
						current = WorkerDecision.needInfo(llm);
						continue;
					}
					if (llm.getStatus() == LlmStatus.ERROR) {
						current = WorkerDecision.error(llm.getMessage());
						continue;
					}
				}

				return true;
			}

			case ASK_USER -> {
				emit(sr, "Asking a question");
				String msg = decision.getInput().get("message").asText();
				sr.addChunk("\n" + msg);
				sr.markComplete();
				return false;
			}

			case ERROR -> {
				sr.addChunk("\nError: " + decision.getReason() + "\n\n");
				sr.markComplete();
				return false;
			}

			default -> throw new IllegalStateException("Unhandled action: " + decision.getAction());
			}
		}

	}

	private String synthesize(UserId userId, AssistantQuery query, PlanState state, StreamResult sr) {
		AgentQuery synthQuery = workerQueryFactoryService.synthesizer(query, state);
		return assistantQueryService.runSingleLlmCallStreaming(userId, synthQuery.query(), sr);
	}

	private void emit(StreamResult sr, String msg) {
		sr.addChunk("[STATUS] " + msg);
	}
}