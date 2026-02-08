package tom.config.model;

import java.net.URI;
import java.time.Duration;
import java.util.List;

public record OllamaConfig(URI uri, String defaultModel, List<ChatModelConfig> chatModels,
		String conversationNamingModel, String diagrammingModel, int chatMemoryDepth, int defaultTopK,
		Duration apiConnectTimeout, Duration apiTimeout, Duration asyncResponseTimeout, EmbeddingConfig embedding) {
}
