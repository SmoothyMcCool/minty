package tom.tasks.transform.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.AssistantId;
import tom.api.services.assistant.AssistantManagementService;
import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;
import tom.tasks.TaskUtils;

public class AiQueryConfig implements TaskConfigSpec {

	private AssistantId assistant;
	private String query;

	public AiQueryConfig() {
		assistant = AssistantManagementService.DefaultAssistantId;
		query = "";
	}

	public AiQueryConfig(Map<String, String> config) {
		this();
		if (config.containsKey("Assistant")) {
			assistant = new AssistantId(config.get("Assistant"));
		}
		if (config.containsKey("Query")) {
			query = config.get("Query");
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Assistant", TaskConfigTypes.EnumList);
		cfg.put("Query", TaskConfigTypes.TextArea);
		return cfg;
	}

	public void updateFrom(Map<String, Object> map) {
		if (map.containsKey("Assistant")) {
			assistant = TaskUtils.safeConvert(map.get("Assistant"), AssistantId.class);
		}
		if (map.containsKey("Query")) {
			query = TaskUtils.safeConvert(map.get("Query"), String.class);
		}
	}

	public AssistantId getAssistant() {
		return assistant;
	}

	public String getQuery() {
		return query;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	@Override
	public Map<String, String> getValues() {
		return Map.of("Assistant", assistant.getValue().toString(), "Query", query);
	}
}
