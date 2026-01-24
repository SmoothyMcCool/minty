package tom.api.tool;

import java.util.Map;

public interface MintyTool {

	String name();

	String description();

	// Guaranteed to be called after all other data is set in the Tool, for example
	// by the ServiceConsumer or ConfigurationConsumer interfaces.
	default void initialize() {
	}

	default void setPluginConfiguration(Map<String, Object> pluginConfiguration) {
	}
}
