package tom.openai.service;

import java.util.Map;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import tom.config.MintyConfigurationImpl;
import tom.config.model.ChatModelConfig;
import tom.config.model.EndpointConfig;
import tom.config.model.LlmConfig;
import tom.config.model.ProviderType;
import tom.llm.service.LlmEndpointService;
import tom.llm.service.LlmProviderRegistrar;

@Component
public class OpenAiProviderRegistrar implements LlmProviderRegistrar {

	private static final Logger logger = LogManager.getLogger(OpenAiProviderRegistrar.class);

	private final LlmConfig llmConfig;
	private final JdbcTemplate vectorJdbcTemplate;
	private final DataSource dataSource;

	public OpenAiProviderRegistrar(MintyConfigurationImpl properties, JdbcTemplate vectorJdbcTemplate,
			DataSource dataSource) {
		this.llmConfig = properties.getConfig().llm();
		this.vectorJdbcTemplate = vectorJdbcTemplate;
		this.dataSource = dataSource;
	}

	@Override
	public void registerEndpoints(Map<String, LlmEndpointService> endpointServices,
			Map<String, String> modelToEndpoint) {
		String embeddingModelName = llmConfig.embedding().model();
		int chatMemoryDepth = llmConfig.chatMemoryDepth();

		for (EndpointConfig ep : llmConfig.endpoints()) {
			if (ep.provider() != ProviderType.OpenAI)
				continue;

			endpointServices.put(ep.name(),
					new OpenAiEndpointService(ep, vectorJdbcTemplate, dataSource, embeddingModelName, chatMemoryDepth));

			logger.info("Registered OpenAI endpoint '{}'", ep.name());
		}

		for (ChatModelConfig model : llmConfig.modelDefinitions()) {
			if (!llmConfig.activeModels().contains(model.name()))
				continue;

			boolean belongsToThisProvider = llmConfig.endpoints().stream()
					.anyMatch(e -> e.name().equals(model.endpoint()) && e.provider() == ProviderType.OpenAI);
			if (!belongsToThisProvider)
				continue;

			modelToEndpoint.put(model.name(), model.endpoint());
			logger.info("Registered OpenAI model '{}' → endpoint '{}'", model.name(), model.endpoint());
		}
	}
}