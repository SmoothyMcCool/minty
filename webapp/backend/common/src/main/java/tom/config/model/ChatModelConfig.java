package tom.config.model;

public record ChatModelConfig(String name, String endpoint, int defaultContext, int maximumContext, int maxConcurrent,
		boolean imageSupport) {
}
