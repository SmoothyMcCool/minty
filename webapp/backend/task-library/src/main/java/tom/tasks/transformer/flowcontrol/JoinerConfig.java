package tom.tasks.transformer.flowcontrol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class JoinerConfig implements TaskConfigSpec {

	private int numInputs;

	public JoinerConfig() {
		numInputs = 1;
	}

	public JoinerConfig(Map<String, String> config) {
		this();
		numInputs = Integer.parseInt(config.get("Number of Inputs"));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Number of Inputs", TaskConfigTypes.Number);
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

}
