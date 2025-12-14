package tom.tasks.emit.packet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class PacketEmitterConfig implements TaskConfigSpec {

	private final String DataToEmit = "Data to Emit";

	String data;

	public PacketEmitterConfig() {
		data = "";
	}

	public PacketEmitterConfig(Map<String, String> config) {
		this();
		if (config.containsKey(DataToEmit)) {
			data = config.get(DataToEmit);
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(DataToEmit, TaskConfigTypes.Packet);
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
		return Map.of(DataToEmit, data);
	}
}
