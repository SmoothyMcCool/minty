package tom.tasks.flowcontrol.merge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class MergeConfig implements TaskConfigSpec {

	public static final String NumInputs = "Number of Inputs";

	private int numInputs;

	public MergeConfig() {
		numInputs = 1;
	}

	public MergeConfig(Map<String, Object> config) {
		this();
		try {
			if (config.containsKey(NumInputs)) {
				numInputs = Integer.parseInt(config.get(NumInputs).toString());
			}
		} catch (Exception e) {
			numInputs = 2;
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(NumInputs, TaskConfigTypes.Number);
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

	public int getNumInputs() {
		return numInputs;
	}

	public void setNumInputs(int numInputs) {
		this.numInputs = numInputs;
	}

	@Override
	public Map<String, Object> getValues() {
		return Map.of(NumInputs, Integer.toString(numInputs));
	}
}
