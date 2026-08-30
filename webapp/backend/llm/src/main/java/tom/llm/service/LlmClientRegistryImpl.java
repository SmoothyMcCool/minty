package tom.llm.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.jdbc.JdbcSessionRepository;
import org.springframework.ai.session.jdbc.JdbcSessionRepositoryDialect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.config.MintyConfigurationImpl;
import tom.config.model.ChatModelConfig;
import tom.config.model.EmbeddingConfig;
import tom.user.model.User;

@Service
public class LlmClientRegistryImpl implements LlmClientRegistry {

	private final Map<String, String> modelToEndpoint;
	private final Map<String, LlmEndpointService> endpointServices;
	private final MintyConfigurationImpl properties;
	private final SessionRepository sessionRepository;
	private final SessionService sessionService;

	public LlmClientRegistryImpl(MintyConfigurationImpl properties, List<LlmProviderRegistrar> registrars,
			JdbcTemplate vectorJdbcTemplate, DataSource dataSource) {
		modelToEndpoint = new HashMap<>();
		endpointServices = new HashMap<>();
		this.properties = properties;
		for (LlmProviderRegistrar registrar : registrars) {
			registrar.registerEndpoints(endpointServices, modelToEndpoint);
		}

		sessionRepository = JdbcSessionRepository.builder().jdbcTemplate(vectorJdbcTemplate)
				.dialect(JdbcSessionRepositoryDialect.from(dataSource)).build();
		sessionService = DefaultSessionService.builder().sessionRepository(sessionRepository)
				.defaultTimeToLive(properties.getConfig().llm().chatMemoryStorageTime()).build();
	}

	@Override
	public ChatClient buildChatClient(User user, Assistant assistant, AssistantQuery query, int contextSize,
			List<Advisor> advisors) {
		return serviceForModel(assistant.model()).buildChatClient(user, assistant, query, contextSize, advisors);
	}

	@Override
	public ChatModel buildSimpleModel(User user, String modelName) {
		return serviceForModel(modelName).buildSimpleModel(user, modelName);
	}

	@Override
	public SessionService getSessionService() {
		return sessionService;
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

	@Override
	public EmbeddingModel getEmbeddingModel(User user, EmbeddingConfig embeddingConfig) {
		LlmEndpointService endpointService = endpointServices.get(embeddingConfig.endpoint());
		return endpointService.buildEmbeddingModel(user, embeddingConfig.model());
	}
}