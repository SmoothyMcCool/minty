package tom.tasks.aiassistant;

import java.util.HashMap;
import java.util.Map;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class AiAssistantTaskConfig implements AiTaskConfig {

	private int assistant;
	private String conversationId;
	private String query;

	public AiAssistantTaskConfig() {
		assistant = 0;
		conversationId = "";
		query = "";
	}

	public AiAssistantTaskConfig(Map<String, String> config) {
		assistant = Integer.parseInt(config.get("Assistant"));
		conversationId = config.get("Conversation ID");
		query = config.get("Prompt");
	}

	@Override
	public Map<String, AiTaskConfigTypes> getConfig() {
		Map<String, AiTaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Assistant", AiTaskConfigTypes.AssistantIdentifier);
		cfg.put("Conversation ID", AiTaskConfigTypes.String);
		cfg.put("Prompt", AiTaskConfigTypes.String);
		return cfg;
	}

	public int getAssistant() {
		return assistant;
	}

	public String getConversationId() {
		return conversationId;
	}

	public String getQuery() {
		return query;
	}
}
