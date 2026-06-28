package tom.anthropic.service;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import tom.config.MintyConfigurationImpl;
import tom.config.model.EndpointConfig;
import tom.config.model.LlmConfig;
import tom.config.model.ProviderType;
import tom.llm.service.LlmEndpointService;
import tom.llm.service.LlmProviderRegistrar;

@Component
public class AnthropicProviderRegistrar implements LlmProviderRegistrar {

	private static final Logger logger = LogManager.getLogger(AnthropicProviderRegistrar.class);

	private final LlmConfig llmConfig;

	public AnthropicProviderRegistrar(MintyConfigurationImpl properties) {
		this.llmConfig = properties.getConfig().llm();
	}

	@Override
	public void registerEndpoints(Map<String, LlmEndpointService> endpointServices,
			Map<String, String> modelToEndpoint) {

		for (EndpointConfig ep : llmConfig.endpoints()) {
			if (ep.provider() != ProviderType.Anthropic)
				continue;

			endpointServices.put(ep.name(), new AnthropicEndpointService(ep));

			logger.info("Registered Anthropic endpoint '{}'", ep.name());
		}

		for (var model : llmConfig.modelDefinitions()) {
			if (!llmConfig.activeModels().contains(model.name()))
				continue;

			boolean belongsToThisProvider = llmConfig.endpoints().stream()
					.anyMatch(e -> e.name().equals(model.endpoint()) && e.provider() == ProviderType.Anthropic);
			if (!belongsToThisProvider)
				continue;

			modelToEndpoint.put(model.name(), model.endpoint());
			logger.info("Registered Anthropic model '{}' → endpoint '{}'", model.name(), model.endpoint());
		}
	}
}