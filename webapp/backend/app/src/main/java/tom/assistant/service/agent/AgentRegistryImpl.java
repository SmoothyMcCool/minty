package tom.assistant.service.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tom.api.ConversationId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.services.AgentRegistry;
import tom.api.services.assistant.AssistantManagementService;
import tom.assistant.service.agent.model.Agent;
import tom.assistant.service.agent.model.AgentInput;
import tom.assistant.service.agent.model.AgentQuery;
import tom.assistant.service.agent.model.AgentResponseType;
import tom.assistant.service.agent.model.PlanState;
import tom.config.MintyConfiguration;
import tom.llm.service.LlmService;

@Service
public class AgentRegistryImpl implements AgentRegistry {

	private static final Logger logger = LogManager.getLogger(AgentRegistryImpl.class);
	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	private final Map<String, Assistant> orchestrators;
	private final Map<String, Agent> staticAgents;
	private final Map<String, Agent> dynamicAgents;
	private final ChatMemory chatMemory;

	public AgentRegistryImpl(MintyConfiguration config, LlmService llmService)
			throws StreamReadException, DatabindException, IOException {
		orchestrators = new HashMap<>();
		staticAgents = new HashMap<>();
		dynamicAgents = new HashMap<>();
		chatMemory = llmService.getChatMemory();

		Path agentRoot = config.getConfig().fileStores().agents();
		Path orchestratorsPath = agentRoot.resolve("orchestrators");
		Path staticAgentsPath = agentRoot.resolve("agents");
		Path dynamicAgentsPath = agentRoot.resolve("dynamic");

		try (Stream<Path> orchestratorsStream = Files.list(orchestratorsPath);
				Stream<Path> staticAgentsStream = Files.list(staticAgentsPath);
				Stream<Path> dynamicAgentsStream = Files.list(dynamicAgentsPath)) {

			orchestratorsStream.filter(Files::isRegularFile).forEach((p) -> {
				Assistant assistant = buildAssistant(p);
				if (assistant != null) {
					orchestrators.put(assistant.name(), assistant);
					logger.info("Added orchestrator " + assistant.name());
				}
			});
			staticAgentsStream.filter(Files::isRegularFile).forEach((p) -> {
				Agent agent = buildAgent(p);
				if (agent != null) {
					staticAgents.put(agent.getName(), agent);
					logger.info("Added static agent " + agent.getName());
				}
			});
			dynamicAgentsStream.filter(Files::isRegularFile).forEach((p) -> {
				Agent agent = buildAgent(p);
				if (agent != null) {
					dynamicAgents.put(agent.getName(), agent);
					logger.info("Added dynamic agent " + agent.getName());
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

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
		return agentQuery;
	}

	private static AgentQuery baseQuery(AgentResponseType responseType, Agent agent, ConversationId conversationId,
			String query) {
		AssistantQuery assistantQuery = new AssistantQuery();
		AssistantSpec spec = new AssistantSpec(null, agent.toAssistant());
		assistantQuery.setAssistantSpec(spec);
		assistantQuery.setContextSize(agent.getContextSize());
		assistantQuery.setConversationId(conversationId);
		assistantQuery.setQuery(query);

		AgentQuery agentQuery = new AgentQuery(responseType, assistantQuery);
		return agentQuery;
	}

	@SuppressWarnings("unchecked")
	private Assistant buildAssistant(Path assistantFilePath) {
		Map<String, Object> data;
		try {
			data = mapper.readValue(assistantFilePath.toFile(), Map.class);
			AssistantBuilder builder = new AssistantBuilder();

			List<String> tools = mapper.convertValue(data.get("tools"), new TypeReference<List<String>>() {
			});

			builder.id(AssistantManagementService.DefaultAssistantId).name((String) data.get("name"))
					.model((String) data.get("model")).contextSize((Integer) data.get("contextSize"))
					.temperature((Double) data.get("temperature")).topK((Integer) data.get("topK"))
					.prompt((String) data.get("prompt")).tools(tools).owned(false) // Assistants from the registry
																					// are never owned by a user.
					.hasMemory((Boolean) data.get("hasMemory")).documentIds(List.of());
			return builder.build();
		} catch (IOException e) {
			logger.warn("Could not read agent file " + assistantFilePath.getFileName());
		}
		return null;
	}

	private Agent buildAgent(Path agentFilePath) {
		try {
			return mapper.readValue(agentFilePath.toFile(), Agent.class);
		} catch (IOException e) {
			logger.warn("Could not read agent file " + agentFilePath.getFileName(), e);
			return null;
		}
	}

	public AgentQuery getPlanner(String plannerName, AssistantQuery userQuery, PlanState state) {
		Assistant assistant = orchestrators.get(plannerName);
		AssistantBuilder builder = assistant.toBuilder();

		List<Message> history = chatMemory.get(userQuery.getConversationId().getValue().toString());
		String chatHistory = history.stream().map(m -> m.getMessageType() + ": " + m.getText())
				.collect(Collectors.joining("\n"));

		String prompt = AgentPlannerPromptBuilder.buildPrompt(assistant.prompt(), staticAgents.values(),
				dynamicAgents.values(), chatHistory, state);
		builder.prompt(prompt);

		return baseQuery(AgentResponseType.Structured, builder.build(), userQuery.getConversationId(),
				userQuery.getQuery());
	}

	public boolean hasAgent(String key) {
		return staticAgents.containsKey(key) || dynamicAgents.containsKey(key);
	}

	public Agent getAgent(String name) {
		Agent agent = staticAgents.get(name);
		if (agent == null) {
			agent = dynamicAgents.get(name);
		}
		return agent;
	}

	public AgentQuery getAgent(String agentName, AssistantQuery userQuery, PlanState state) {
		Agent agent = getAgent(agentName);
		if (agent == null) {
			throw new IllegalArgumentException("Unknown agent: " + agentName);
		}

		AgentInput input = AgentInput.resolve(userQuery, state);
		String stepName = state != null ? "[" + state.currentStep().left().getName() + "]" : "";
		return baseQuery(agent.getResponseType(), agent, userQuery.getConversationId(), stepName + input.toPrompt());
	}

	@Override
	public String getAgentDescription(String agentName) {
		Agent agent = getAgent(agentName);
		if (agent == null) {
			return "Agent not found.";
		}
		return AgentPlannerPromptBuilder.createAgentDefinition(agent);
	}
}
