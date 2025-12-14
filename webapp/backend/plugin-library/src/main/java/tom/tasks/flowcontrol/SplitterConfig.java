package tom.tasks.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class SplitterConfig implements TaskConfigSpec {

	private final String NumOutputs = "Number of Outputs";

	private int numOutputs;

	public SplitterConfig() {
		numOutputs = 2;
	}

	public SplitterConfig(Map<String, String> config) {
		this();
		try {
			if (config.containsKey(NumOutputs)) {
				numOutputs = Integer.parseInt(config.get(NumOutputs));
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

	public void setNumOutputs(int numOutputs) {
		this.numOutputs = numOutputs;
	}

	@Override
	public Map<String, String> getValues() {
		return Map.of(NumOutputs, Integer.toString(numOutputs));
	}
}
