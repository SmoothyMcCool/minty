package tom.config.model;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public record LlmConfig(LlmEngine engine, URI uri, List<ChatModelConfig> modelDefinitions, List<String> activeModels,
		String conversationNamingModel, String diagrammingModel, int chatMemoryDepth, int defaultTopK,
		Duration apiConnectTimeout, Duration apiTimeout, Duration asyncResponseTimeout, EmbeddingConfig embedding) {
}
