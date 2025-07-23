package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class AiQueryTaskConfig implements TaskConfig {

	private int assistant;
	private String conversationId;
	private String query;

	public AiQueryTaskConfig() {
		assistant = 0;
		conversationId = "";
		query = "";
	}

	public AiQueryTaskConfig(Map<String, String> config) {
		assistant = Integer.parseInt(config.get("Assistant"));
		conversationId = config.get("Conversation ID");
		query = config.get("Prompt");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Assistant", TaskConfigTypes.AssistantIdentifier);
		cfg.put("Conversation ID", TaskConfigTypes.String);
		cfg.put("Prompt", TaskConfigTypes.String);
		return cfg;
	}

	public void updateFrom(Map<String, String> map) {
		if (map.containsKey("Assistant")) {
			assistant = Integer.parseInt(map.get("Assistant"));
		}
		if (map.containsKey("Conversation ID")) {
			conversationId = map.get("Conversation ID");
		}
		if (map.containsKey("Prompt")) {
			query = map.get("Prompt");
		}
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
