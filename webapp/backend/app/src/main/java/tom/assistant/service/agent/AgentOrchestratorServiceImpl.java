package tom.assistant.service.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private AssistantQueryService assistantQueryService;
	private final WorkerQueryFactoryService workerQueryFactoryService;
	private final AgentPlanner planner;

	public AgentOrchestratorServiceImpl(WorkerQueryFactoryService workerQueryFactoryService, AgentPlanner planner) {
		this.workerQueryFactoryService = workerQueryFactoryService;
		this.planner = planner;
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
			runGeneralWorker(userId, query, sr);
			sr.markComplete();
			return;
		}

		emit(sr, "Steps planned. Running" + steps.size() + " steps.");
		for (AgentStep step : steps) {
			emit(sr, step.getName());
		}
		for (AgentStep step : steps) {

			emit(sr, "Running step: " + step.getName());

			try {
				Object result = runStep(userId, query, step, state, sr);
				state.put(step.getId(), result);

			} catch (Exception e) {
				emit(sr, "Step failed: " + step.getWorker());
				sr.addChunk("\nError: " + e.getMessage());
				break;
			}
		}

		emit(sr, "Finalizing response...");

		String finalAnswer = synthesize(userId, query, state);

		sr.addChunk("\n" + finalAnswer);
		sr.markComplete();
	}

	private Object runStep(UserId userId, AssistantQuery originalQuery, AgentStep step, Map<String, Object> state,
			StreamResult sr) {

		return switch (step.getWorker()) {

		case "general" -> runGeneralWorker(userId, originalQuery, sr);

		// case "diagram_parser" -> runDiagramParser(userId, step, sr);

		// case "mermaid_generator" -> runMermaidGenerator(userId, step, state, sr);

		// case "mermaid_validator" -> runValidator(step, state, sr);

		case "workflowPlanner" -> runWorkflowPlanner(step, state, sr);

		default -> throw new IllegalArgumentException("Unknown worker: " + step.getWorker());
		};
	}

	// --- Workers ---

	private String runGeneralWorker(UserId userId, AssistantQuery query, StreamResult sr) {
		return assistantQueryService.runSingleLlmCallStreaming(userId, query, sr);
	}

	private Object runDiagramParser(UserId userId, AgentStep step, StreamResult sr) {
		AssistantQuery workerQuery = workerQueryFactoryService.diagramParser(step);
		return assistantQueryService.runSingleLlmCall(userId, workerQuery);
	}

	private Object runMermaidGenerator(UserId userId, AgentStep step, Map<String, Object> state, StreamResult sr) {
		AssistantQuery workerQuery = workerQueryFactoryService.mermaidGenerator(step, state);
		return assistantQueryService.runSingleLlmCall(userId, workerQuery);
	}

	private Object runValidator(AgentStep step, Map<String, Object> state, StreamResult sr) {
		String mermaid = (String) state.get(step.getInput().get("fromStep"));
		// TODO: plug real validator
		return Map.of("valid", true, "errors", List.of());
	}

	private Object runWorkflowPlanner(AgentStep step, Map<String, Object> state, StreamResult sr) {
		AssistantQuery workflow = workerQueryFactoryService.workflowPlanner(step, state);

		// TODO: plug real validator
		return Map.of("valid", true, "errors", List.of());
	}

	private String synthesize(UserId userId, AssistantQuery query, Map<String, Object> state) {
		AssistantQuery synthQuery = workerQueryFactoryService.synthesizer(query, state);
		return assistantQueryService.runSingleLlmCall(userId, synthQuery);
	}

	private void emit(StreamResult sr, String msg) {
		sr.addChunk("[STATUS] " + msg + "\n\n");
	}
}