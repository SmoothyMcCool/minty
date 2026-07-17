package tom.config.model;

import java.time.Duration;
import java.util.List;

public record LlmConfig(List<EndpointConfig> endpoints, List<ChatModelConfig> modelDefinitions,
		List<String> activeModels, int chatMemoryDepth, int defaultTopK, Duration apiConnectTimeout,
		Duration apiTimeout, Duration asyncResponseTimeout, String conversationNamingModel, EmbeddingConfig embedding) {
}
