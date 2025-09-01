package tom.tasks.transformer.emitter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class DataEmitterConfig implements TaskConfig {

	String keyName;
	List<String> data = List.of();

	public DataEmitterConfig() {
	}

	public DataEmitterConfig(Map<String, String> config) {
		keyName = config.get("Key Name");
		data = Arrays.asList(config.get("Data to Emit").split(",")).stream().map(item -> item.trim()).toList();
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Key Name", TaskConfigTypes.String);
		config.put("Data to Emit", TaskConfigTypes.String);
		return config;
	}

	List<String> getData() {
		return data;
	}

	String getKeyName() {
		return keyName;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of();
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of();
	}
}
