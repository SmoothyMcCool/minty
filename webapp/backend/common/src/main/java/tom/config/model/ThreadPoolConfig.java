package tom.config.model;

public record ThreadPoolConfig(int taskDefault, int taskMax, int llmDefault, int llmMax, int documentDefault,
		int documentMax, int streamDefault, int streamMax, int streamCapacity) {
}
