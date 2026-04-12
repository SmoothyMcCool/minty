package tom.assistant.service.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;
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

		Map<String, Object> state = new ConcurrentHashMap<>();

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
		plan.append("Running plan (" + steps.size() + " steps):  \n");
		for (AgentStep step : steps) {
			plan.append("1. ").append(step.getName()).append("(" + step.getWorker() + ")\n");
		}
		emit(sr, plan.toString());

		for (AgentStep step : steps) {

			emit(sr, "Running step: " + step.getName());

			try {
				WorkerDecision decision = runStep(userId, query, step, state, sr);
				emit(sr, "Decision: " + decision.toString());

				boolean shouldContinue = handleDecision(userId, query, step, decision, state, sr);
				if (!shouldContinue) {
					return;
				}

			} catch (Exception e) {
				emit(sr, "Step failed: " + step.getWorker());
				sr.addChunk("\nError: " + e.getMessage() + "\n\n");
				break;
			}
		}

		emit(sr, "Finalizing response...");

		String finalAnswer = synthesize(userId, query, state, sr);

		sr.addChunk("\n" + finalAnswer);
		sr.markComplete();
	}

	private WorkerDecision runStep(UserId userId, AssistantQuery originalQuery, AgentStep step,
			Map<String, Object> state, StreamResult sr) {

		WorkerHandler handler = workerRegistry.get(step.getWorker());
		WorkerContext context = new WorkerContext(userId, originalQuery, step, state, sr);

		return handler.handle(context); // returns WorkerDecision
	}

	private boolean handleDecision(UserId userId, AssistantQuery originalQuery, AgentStep step, WorkerDecision decision,
			Map<String, Object> state, StreamResult sr) {

		WorkerDecision current = decision;

		while (true) {
			switch (current.getAction()) {

			case LLM_CALL -> {

				String type = decision.getInput().get("type").asText();

				AgentQuery q = switch (type) {
				case "general" -> workerQueryFactoryService.general(originalQuery);
				case "diagram_parser" -> workerQueryFactoryService.diagramParser(originalQuery);
				case "mermaid_generate" -> workerQueryFactoryService.mermaidGenerator(originalQuery, state);
				case "mermaid_validate" -> workerQueryFactoryService.mermaidValidator(originalQuery, state);
				case "plan_workflow" -> workerQueryFactoryService.workflowPlanner(originalQuery, state);
				default -> throw new IllegalStateException("Unknown LLM call type: " + type);
				};

				// Call LLM
				String raw = assistantQueryService.runSingleLlmCall(userId, q.query());

				// Deserialize into LlmResponse
				LlmResponse llm;
				try {
					llm = LlmResponse.parse(raw, q.responsetype());
				} catch (JsonProcessingException e) {
					emit(sr, "Agent returned an invalid response: " + raw);
					return false;
				}

				// Store structured result
				state.put(type, llm);
				/*
				 * switch (type) { case "diagram_parser" -> state.put("diagram_parser", llm);
				 * case "mermaid_generate" -> state.put("mermaid_generate", llm); case
				 * "mermaid_validate" -> state.put("mermaid_validate", llm); }
				 */

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

			case RESULT -> {
				state.put("result", decision.getInput());
				return true;
			}

			case ASK_USER -> {
				emit(sr, "Asking a question");
				String msg = decision.getInput().get("message").asText();
				sr.addChunk("\n" + msg);
				sr.markComplete();
				return false;
			}

			case DONE -> {
				emit(sr, "Done");
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

	private String synthesize(UserId userId, AssistantQuery query, Map<String, Object> state, StreamResult sr) {
		AgentQuery synthQuery = workerQueryFactoryService.synthesizer(query, state);
		return assistantQueryService.runSingleLlmCallStreaming(userId, synthQuery.query(), sr);
	}

	private void emit(StreamResult sr, String msg) {
		sr.addChunk("[STATUS] " + msg + "\n\n");
	}
}