package tom.assistant.service.agent;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.services.assistant.AssistantRegistryService;

@Service
public class WorkerQueryFactoryService {

	private final AssistantRegistryService assistantRegistryService;

	public WorkerQueryFactoryService(AssistantRegistryService assistantRegistryService) {
		this.assistantRegistryService = assistantRegistryService;
	}

	private static AssistantQuery baseQuery(Assistant assistant, String query) {
		AssistantQuery assistantQuery = new AssistantQuery();
		AssistantSpec spec = new AssistantSpec(null, assistant);
		assistantQuery.setAssistantSpec(spec);
		assistantQuery.setContextSize(assistant.contextSize());
		assistantQuery.setConversationId(new ConversationId(UUID.randomUUID()));
		assistantQuery.setQuery(query);
		return assistantQuery;
	}

	// --- WORKERS ---

	public AssistantQuery planner(String userQuery, int contextSize) {

		Assistant assistant = assistantRegistryService.createConversationPlannerAssistant();
		AssistantBuilder builder = assistant.toBuilder();
		builder.prompt(null);
		String query = assistant.prompt().replace("{{USER_QUERY}}", userQuery);

		return baseQuery(builder.build(), query);
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

		Assistant assistant = assistantRegistryService.createConversationPlannerAssistant();
		AssistantBuilder builder = assistant.toBuilder();
		builder.prompt(null);
		String query = assistant.prompt().replace("{{STATE}}", state.toString()).replace("{{USER_QUERY}}",
				original.getQuery());

		return baseQuery(builder.build(), query);
	}
}