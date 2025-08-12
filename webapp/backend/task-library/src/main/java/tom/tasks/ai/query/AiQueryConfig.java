package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class AiQueryConfig implements TaskConfig {

	private int assistant;
	private String query;

	public AiQueryConfig() {
		assistant = 0;
		query = "";
	}

	public AiQueryConfig(Map<String, String> config) {
		assistant = Integer.parseInt(config.get("Assistant"));
		query = config.get("Prompt");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Assistant", TaskConfigTypes.AssistantIdentifier);
		cfg.put("Prompt", TaskConfigTypes.String);
		return cfg;
	}

	public void updateFrom(Map<String, String> map) {
		if (map.containsKey("Assistant")) {
			assistant = Integer.parseInt(map.get("Assistant"));
		}
		if (map.containsKey("Prompt")) {
			query = map.get("Prompt");
		}
	}

	public int getAssistant() {
		return assistant;
	}

	public String getQuery() {
		return query;
	}
}
