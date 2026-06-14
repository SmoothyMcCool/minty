package tom.llm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.MintyConfigurationImpl;
import tom.config.model.ChatModelConfig;
import tom.user.model.User;

@Service
public class LlmClientRegistryImpl implements LlmClientRegistry {

	private final Map<String, String> modelToEndpoint;
	private final Map<String, LlmEndpointService> endpointServices;
	private final MintyConfigurationImpl properties;
	private final ChatMemoryRepository chatMemoryRepository;
	private final ChatMemory chatMemory;

	public LlmClientRegistryImpl(MintyConfigurationImpl properties, List<LlmProviderRegistrar> registrars,
			JdbcTemplate vectorJdbcTemplate, DataSource dataSource) {
		modelToEndpoint = new HashMap<>();
		endpointServices = new HashMap<>();
		this.properties = properties;

		for (LlmProviderRegistrar registrar : registrars) {
			registrar.registerEndpoints(endpointServices, modelToEndpoint);
		}

		chatMemoryRepository = JdbcChatMemoryRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();

		int chatMemoryDepth = properties.getConfig().llm().chatMemoryDepth();
		chatMemory = MessageWindowChatMemory.builder().maxMessages(chatMemoryDepth)
				.chatMemoryRepository(chatMemoryRepository).build();

	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		return serviceForModel(assistant.model()).buildChatClient(user, assistant, query, contextSize, advisors);
	}

	@Override
	public ChatModel buildSimpleModel(String modelName) {
		return serviceForModel(modelName).buildSimpleModel(modelName);
	}

	@Override
	public VectorStore getVectorStore(String modelName) {
		return serviceForModel(modelName).getVectorStore();
	}

	@Override
	public ChatMemoryRepository getChatMemoryRepository() {
		return chatMemoryRepository;
	}

	@Override
	public ChatMemory getChatMemory() {
		return chatMemory;
	}

	@Override
	public boolean has(String modelName) {
		return modelToEndpoint.containsKey(modelName);
	}

	@Override
	public Set<String> modelNames() {
		return modelToEndpoint.keySet();
	}

	@Override
	public List<ChatModelConfig> listModels() {
		return properties.getConfig().llm().modelDefinitions().stream()
				.filter(model -> modelToEndpoint.containsKey(model.name())).toList();
	}

	@Override
	public LlmEndpointService serviceForModel(String modelName) {
		String endpointName = Optional.ofNullable(modelToEndpoint.get(modelName))
				.orElseThrow(() -> new IllegalArgumentException("No endpoint registered for model: " + modelName));
		return endpointServices.get(endpointName);
	}

}