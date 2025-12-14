package tom.tasks.noop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class NullTaskConfig implements TaskConfigSpec {

	public NullTaskConfig() {
	}

	public NullTaskConfig(Map<String, String> config) {
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		return new HashMap<>();
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
		return Map.of();
	}
}
