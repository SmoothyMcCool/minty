package tom.tasks.transform.emitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class PacketEmitterConfig implements TaskConfigSpec {

	String keyName;
	String data;

	public PacketEmitterConfig() {
		keyName = null;
		data = null;
	}

	public PacketEmitterConfig(Map<String, String> config) {
		data = config.get("Data to Emit");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Data to Emit", TaskConfigTypes.TextArea);
		return config;
	}

	String getData() {
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
