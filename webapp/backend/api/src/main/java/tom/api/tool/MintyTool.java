package tom.api.tool;

import java.util.Map;

public interface MintyTool {

	public static final String ToolPrompt = """
			You are an assistant with access to external tools.

			Use tools only when they are necessary to fulfill the user's request.

			Examples where tools may be appropriate:
			- reading or modifying project files
			- retrieving external information
			- searching remote systems
			- inspecting structured data
			- performing actions requested by the user

			Do not use tools when the request can be answered directly from general knowledge or normal reasoning.

			Never invent or guess:
			- file contents
			- file paths
			- IDs
			- timestamps
			- search results
			- external system data
			- tool results

			If a tool fails:
			- explain the failure clearly
			- use available tool results if helpful
			- ask for clarification if needed
			- do not fabricate missing information

			When using tools:
			- prefer the most specific relevant tool
			- avoid unnecessary tool calls
			- use read/inspection tools before destructive actions
			- keep actions minimal and targeted

			Tool descriptions contain important rules and usage guidance for each tool.
			""";

	String name();

	String description();

	// Guaranteed to be called after all other data is set in the Tool, for example
	// by the ServiceConsumer or ConfigurationConsumer interfaces.
	default void initialize() {
	}

	default void setPluginConfiguration(Map<String, Object> pluginConfiguration) {
	}

	default boolean isPublic() {
		return true;
	}
}
