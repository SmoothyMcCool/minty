package tom.tasks.transformer.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class SplitterConfig implements TaskConfigSpec {

	private int numOutputs;

	public SplitterConfig() {
		numOutputs = 1;
	}

	public SplitterConfig(Map<String, String> config) {
		this();
		numOutputs = Integer.parseInt(config.get("Number of Outputs"));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Number of Outputs", TaskConfigTypes.Number);
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

}
