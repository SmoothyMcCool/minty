package tom.assistant.service.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.StreamResult;
import tom.assistant.service.agent.model.AgentAction;
import tom.assistant.service.agent.model.AgentResponse;
import tom.assistant.service.agent.model.AgentResponseDeserializer;
import tom.assistant.service.agent.model.AgentResponseTypeRegistry;
import tom.assistant.service.agent.model.AskUserResponse;

@Service
public class AgentOrchestratorServiceImpl implements AgentOrchestratorService {

	private AssistantQueryService assistantQueryService;
	private final WorkerQueryFactoryService workerQueryFactoryService;
	private final AgentPlanner planner;
	private final ObjectMapper mapper;

	public AgentOrchestratorServiceImpl(WorkerQueryFactoryService workerQueryFactoryService, AgentPlanner planner) {
		this.workerQueryFactoryService = workerQueryFactoryService;
		this.planner = planner;

		AgentResponseTypeRegistry registry = new AgentResponseTypeRegistry();
		registry.register(AgentAction.ASK_USER, AskUserResponse.class);
		// registry.register("delete_user", DeleteUserPayload.class);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(AgentResponse.class, new AgentResponseDeserializer(registry));

		mapper = new ObjectMapper();
		mapper.registerModule(module);

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

		emit(sr, "Running plan (" + steps.size() + " steps):");
		StringBuilder plan = new StringBuilder();
		for (AgentStep step : steps) {
			plan.append("\t").append(step.getName()).append("\n");
		}
		emit(sr, plan.toString());

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