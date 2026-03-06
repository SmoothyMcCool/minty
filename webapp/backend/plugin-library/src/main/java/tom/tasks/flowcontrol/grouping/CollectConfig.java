package tom.tasks.flowcontrol.grouping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.Grouping;
import tom.tasks.GroupingEnumSpecCreator;

public class CollectConfig implements TaskConfigSpec {

	private Grouping grouping;

	public CollectConfig() {
		grouping = Grouping.All;
	}

	public CollectConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(GroupingEnumSpecCreator.EnumName)) {
			grouping = Grouping.valueOf(config.get(GroupingEnumSpecCreator.EnumName).toString());
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(GroupingEnumSpecCreator.EnumName, TaskConfigTypes.EnumList);
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

	@Override
	public Map<String, Object> getValues() {
		return Map.of(GroupingEnumSpecCreator.EnumName, grouping.toString());
	}
}
