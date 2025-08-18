package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;
import tom.tasks.TaskUtils;

public class AiQueryConfig implements TaskConfig {

	private Integer assistant;
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

	public void updateFrom(Map<String, Object> map) {
		if (map.containsKey("Assistant")) {
			assistant = TaskUtils.safeConvert(map.get("Assistant"), Integer.class);
		}
		if (map.containsKey("Prompt")) {
			query = TaskUtils.safeConvert(map.get("Prompt"), String.class);
		}
	}

	public int getAssistant() {
		return assistant == null ? 0 : assistant;
	}

	public String getQuery() {
		return query;
	}
}
