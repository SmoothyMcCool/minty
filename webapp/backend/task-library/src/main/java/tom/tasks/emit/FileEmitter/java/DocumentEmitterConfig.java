package tom.tasks.emit.FileEmitter.java;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class DocumentEmitterConfig implements TaskConfigSpec {

	String base64;

	public DocumentEmitterConfig() {
		base64 = null;
	}

	public DocumentEmitterConfig(Map<String, String> config) {
		base64 = config.get("File");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("File", TaskConfigTypes.Document);
		return config;
	}

	String getBase64() {
		return base64;
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
