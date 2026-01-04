package tom.config.model;

public record ChatModelConfig(String name, int defaultContext, int maximumContext, int maxConcurrent,
		boolean imageSupport) {
}
