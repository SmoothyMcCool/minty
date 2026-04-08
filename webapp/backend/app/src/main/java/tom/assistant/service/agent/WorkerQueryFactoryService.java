package tom.assistant.service.agent;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.assistant.service.management.AssistantRegistry;

@Service
public class WorkerQueryFactoryService {

	private final AssistantRegistry assistantRegistry;

	public WorkerQueryFactoryService(AssistantRegistry assistantRegistry) {
		this.assistantRegistry = assistantRegistry;
	}

	private static AssistantQuery baseQuery(Assistant assistant, ConversationId conversationId, String query) {
		AssistantQuery assistantQuery = new AssistantQuery();
		AssistantSpec spec = new AssistantSpec(null, assistant);
		assistantQuery.setAssistantSpec(spec);
		assistantQuery.setContextSize(assistant.contextSize());
		assistantQuery.setConversationId(conversationId);
		assistantQuery.setQuery(query);
		return assistantQuery;
	}

	// --- WORKERS ---

	public AssistantQuery planner(String userQuery, ConversationId conversationId, int contextSize) {

		Assistant assistant = assistantRegistry.getOrchestrator("planner");
		return baseQuery(assistant, conversationId, userQuery);
	}

	public static AssistantQuery diagramParser(AgentStep step) {
		return null;
		// String prompt = WorkerPrompts.DIAGRAM_PARSER;

		// return baseQuery(prompt);
	}

	public static AssistantQuery mermaidGenerator(AgentStep step, Map<String, Object> state) {
		return null;
		// Object input = state.get(step.getInput().get("fromStep"));

		// String prompt = WorkerPrompts.MERMAID_GENERATOR.replace("{{INPUT}}",
		// String.valueOf(input));

		// return baseQuery(prompt);
	}

	public AssistantQuery synthesizer(AssistantQuery original, Map<String, Object> state) {

		Assistant assistant = assistantRegistry.getWorker("synthesizer");
		String query = """
				State:
				{{STATE}}

				User request:
				{{USER_QUERY}}
				""";
		query = query.replace("{{STATE}}", state.toString()).replace("{{USER_QUERY}}", original.getQuery());

		return baseQuery(assistant, original.getConversationId(), query);
	}

	public AssistantQuery workflowPlanner(AgentStep step, Map<String, Object> state) {
		Assistant assistant = assistantRegistry.getWorker("workflow_planner");

		Object input = state.get(step.getInput().get("fromStep"));
		String query = String.valueOf(input);

		return baseQuery(assistant, new ConversationId(UUID.randomUUID()), query);
	}
}