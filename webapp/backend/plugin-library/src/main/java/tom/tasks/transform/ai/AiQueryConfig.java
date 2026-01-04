package tom.tasks.transform.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.model.assistant.AssistantSpec;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.TaskUtils;

public class AiQueryConfig implements TaskConfigSpec {

	public static final String Assistant = "Assistant";
	public static final String Query = "Query";

	private AssistantSpec assistant;
	private String query;

	public AiQueryConfig() {
		assistant = new AssistantSpec();
		query = "";
	}

	public AiQueryConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(Assistant)) {
			assistant = TaskUtils.safeConvert(config.get(Assistant), AssistantSpec.class);
		}
		if (config.containsKey(Query)) {
			query = config.get(Query).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put(Assistant, TaskConfigTypes.Assistant);
		cfg.put(Query, TaskConfigTypes.TextArea);
		return cfg;
	}

	public void updateFrom(Map<String, Object> map) {
		if (map.containsKey(Assistant)) {
			assistant = TaskUtils.safeConvert(map.get(Assistant), AssistantSpec.class);
		}
		if (map.containsKey(Query)) {
			query = TaskUtils.safeConvert(map.get(Query), String.class);
		}
	}

	public AssistantSpec getAssistant() {
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
	public Map<String, Object> getValues() {
		return Map.of(Assistant, assistant != null ? assistant.toJson() : "", Query, query);
	}
}
