package tom.tasks.transform.collecttext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.Grouping;

public class CollectTextConfig implements TaskConfigSpec {

	public static final String GroupingLabel = "Grouping";
	public static final String Separator = "Separator";

	private Grouping grouping;
	private String separator;

	public CollectTextConfig() {
		grouping = Grouping.All;
		separator = "";
	}

	public CollectTextConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(GroupingLabel)) {
			grouping = Grouping.valueOf(config.get(GroupingLabel).toString());
		}
		if (config.containsKey(Separator)) {
			separator = config.get(Separator).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(GroupingLabel, TaskConfigTypes.EnumList);
		config.put(Separator, TaskConfigTypes.TextArea);
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
	public Map<String, Object> getValues() {
		return Map.of(GroupingLabel, grouping.toString(), Separator, separator);
	}
}
