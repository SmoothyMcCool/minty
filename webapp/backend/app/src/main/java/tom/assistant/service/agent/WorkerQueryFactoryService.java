package tom.assistant.service.agent;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentResponseType;
import tom.assistant.service.agent.model.PlanState;
import tom.assistant.service.management.AssistantRegistry;

@Service
public class WorkerQueryFactoryService {

	private static final Logger logger = LogManager.getLogger(WorkerQueryFactoryService.class);
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

		AgentQuery agentQuery = new AgentQuery(responseType, assistantQuery);
		logger.debug("Prepared Agent Query: " + agentQuery.toString());
		return agentQuery;
	}

	private static String buildQuery(AssistantQuery userQuery, PlanState state) {
		String query = """
				State:
				{{STATE}}

				User request:
				{{USER_QUERY}}
				""";
		return query.replace("{{STATE}}", state.toString()).replace("{{USER_QUERY}}", userQuery.getQuery());
	}

	public AgentQuery planner(String userQuery, ConversationId conversationId, int contextSize) {
		Assistant assistant = assistantRegistry.getOrchestrator("planner");
		return baseQuery(AgentResponseType.Structured, assistant, conversationId, userQuery);
	}

	public AgentQuery general(AssistantQuery userQuery) {
		Assistant assistant = assistantRegistry.getWorker("general");
		return baseQuery(AgentResponseType.RawText, assistant, userQuery.getConversationId(), userQuery.getQuery());
	}

	public AgentQuery general(AssistantQuery userQuery, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("general");
		String query = buildQuery(userQuery, state);
		return baseQuery(AgentResponseType.RawText, assistant, userQuery.getConversationId(), query);
	}

	public AgentQuery diagramParser(AssistantQuery original, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("diagram_parser");
		String query = buildQuery(original, state);
		return baseQuery(AgentResponseType.Structured, assistant, original.getConversationId(), query);
	}

	public AgentQuery mermaidGenerator(AssistantQuery original, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("mermaid_generator");
		String query = buildQuery(original, state);
		return baseQuery(AgentResponseType.RawText, assistant, original.getConversationId(), query);
	}

	public AgentQuery mermaidValidator(AssistantQuery original, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("mermaid_validator");
		String query = buildQuery(original, state);
		return baseQuery(AgentResponseType.Structured, assistant, original.getConversationId(), query);
	}

	public AgentQuery synthesizer(AssistantQuery original, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("synthesizer");
		String query = buildQuery(original, state);
		return baseQuery(AgentResponseType.RawText, assistant, original.getConversationId(), query);
	}

	public AgentQuery workflowPlanner(AssistantQuery original, PlanState state) {
		Assistant assistant = assistantRegistry.getWorker("workflow_planner");
		String query = buildQuery(original, state);
		return baseQuery(AgentResponseType.Structured, assistant, new ConversationId(UUID.randomUUID()), query);
	}
}