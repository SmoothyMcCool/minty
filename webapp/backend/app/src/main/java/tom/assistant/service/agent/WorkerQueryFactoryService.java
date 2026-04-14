package tom.assistant.service.agent;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentResponseType;
import tom.assistant.service.management.AssistantRegistry;

@Service
public class WorkerQueryFactoryService {

	private final AssistantRegistry assistantRegistry;

	public WorkerQueryFactoryService(AssistantRegistry assistantRegistry) {
		this.assistantRegistry = assistantRegistry;
	}

	private static AgentQuery baseQuery(AgentResponseType responseType, Assistant assistant,
			ConversationId conversationId, String query) {
		AssistantQuery assistantQuery = new AssistantQuery();
		AssistantSpec spec = new AssistantSpec(null, assistant);
		assistantQuery.setAssistantSpec(spec);
		assistantQuery.setContextSize(assistant.contextSize());
		assistantQuery.setConversationId(conversationId);
		assistantQuery.setQuery(query);
		return new AgentQuery(responseType, assistantQuery);
	}

	// --- WORKERS ---

	public AgentQuery planner(String userQuery, ConversationId conversationId, int contextSize) {
		Assistant assistant = assistantRegistry.getOrchestrator("planner");
		return baseQuery(AgentResponseType.Structured, assistant, conversationId, userQuery);
	}

	public AgentQuery general(AssistantQuery userQuery) {
		Assistant assistant = assistantRegistry.getWorker("general");
		return baseQuery(AgentResponseType.RawText, assistant, userQuery.getConversationId(), userQuery.getQuery());
	}

	public AgentQuery diagramParser(AssistantQuery original) {
		Assistant assistant = assistantRegistry.getWorker("diagram_parser");

		String query = """
				Parse the following diagram into a structured JSON model.

				Input:
				%s
				""".formatted(original.getQuery());

		return baseQuery(AgentResponseType.Structured, assistant, original.getConversationId(), query);
	}

	public AgentQuery mermaidGenerator(AssistantQuery original, Map<String, Object> state) {
		Assistant assistant = assistantRegistry.getWorker("mermaid_generator");

		Object parsed = state.get("diagram_parser");

		String query = """
				Convert this structured diagram into Mermaid:

				%s
				""".formatted(parsed);

		return baseQuery(AgentResponseType.RawText, assistant, original.getConversationId(), query);
	}

	public AgentQuery mermaidValidator(AssistantQuery original, Map<String, Object> state) {
		Assistant assistant = assistantRegistry.getWorker("mermaid_validator");

		Object parsed = state.get("mermaid_generator");

		String query = """
				Convert this structured diagram into Mermaid:

				%s
				""".formatted(parsed);

		return baseQuery(AgentResponseType.Structured, assistant, original.getConversationId(), query);
	}

	public AgentQuery synthesizer(AssistantQuery original, Map<String, Object> state) {

		Assistant assistant = assistantRegistry.getWorker("synthesizer");
		String query = """
				State:
				{{STATE}}

				User request:
				{{USER_QUERY}}
				""";
		query = query.replace("{{STATE}}", state.toString()).replace("{{USER_QUERY}}", original.getQuery());

		return baseQuery(AgentResponseType.RawText, assistant, original.getConversationId(), query);
	}

	public AgentQuery workflowPlanner(AssistantQuery original, Map<String, Object> state) {
		Assistant assistant = assistantRegistry.getWorker("workflow_planner");

		// Object input = state.get(step.getInput().get("fromStep"));
		String query = String.valueOf(state);

		return baseQuery(AgentResponseType.Structured, assistant, new ConversationId(UUID.randomUUID()), query);
	}
}