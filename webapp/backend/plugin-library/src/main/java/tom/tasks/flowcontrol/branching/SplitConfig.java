package tom.tasks.flowcontrol.branching;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class SplitConfig implements TaskConfigSpec {

	public static final String NumOutputs = "Number of Outputs";

	private int numOutputs;

	public SplitConfig() {
		numOutputs = 2;
	}

	public SplitConfig(Map<String, Object> config) {
		this();
		try {
			if (config.containsKey(NumOutputs)) {
				numOutputs = Integer.parseInt(config.get(NumOutputs).toString());
			}
		} catch (Exception e) {
			numOutputs = 2;
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(NumOutputs, TaskConfigTypes.Number);
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

	public int getNumOutputs() {
		return numOutputs;
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(NumOutputs, Integer.toString(numOutputs));
	}
}
