package tom.config.model;

public record EmbeddingConfig(String model, String summarizingModel, int keywordsPerDocument, String encoding,
		int macroTargetChunkSize, int documentTargetChunkSize, int batchSize, int maxEmbeddingTokens) {
}
