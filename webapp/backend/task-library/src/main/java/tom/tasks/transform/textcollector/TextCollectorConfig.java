package tom.tasks.transform.textcollector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;
import tom.tasks.Grouping;

public class TextCollectorConfig implements TaskConfigSpec {

	private Grouping grouping;
	private String separator;

	public TextCollectorConfig() {
		grouping = Grouping.All;
		separator = "";
	}

	public TextCollectorConfig(Map<String, String> config) {
		this();
		if (config.containsKey("Grouping")) {
			grouping = Grouping.valueOf(config.get("Grouping"));
		}
		if (config.containsKey("Separator")) {
			separator = config.get("Separator");
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Grouping", TaskConfigTypes.EnumList);
		config.put("Separator", TaskConfigTypes.TextArea);
		return config;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}

	public Grouping getGrouping() {
		return grouping;
	}

	public String getSeparator() {
		return separator;
	}

	@Override
	public Map<String, String> getValues() {
		return Map.of("Grouping", grouping.toString(), "Separator", separator);
	}
}
