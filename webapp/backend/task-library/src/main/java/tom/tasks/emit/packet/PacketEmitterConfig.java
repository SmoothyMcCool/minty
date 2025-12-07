package tom.tasks.emit.packet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class PacketEmitterConfig implements TaskConfigSpec {

	String data;

	public PacketEmitterConfig() {
		data = "";
	}

	public PacketEmitterConfig(Map<String, String> config) {
		this();
		if (config.containsKey("Data to Emit")) {
			data = config.get("Data to Emit");
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Data to Emit", TaskConfigTypes.Packet);
		return config;
	}

	String getData() {
		return data;
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
		return Map.of("Data to Emit", data);
	}
}
