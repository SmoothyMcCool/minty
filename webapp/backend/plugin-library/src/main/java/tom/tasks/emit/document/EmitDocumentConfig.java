package tom.tasks.emit.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class EmitDocumentConfig implements TaskConfigSpec {

	public static final String File = "File";
	String base64;

	public EmitDocumentConfig() {
		base64 = "";
	}

	public EmitDocumentConfig(Map<String, Object> config) {
		if (config.containsKey(File)) {
			base64 = config.get(File).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(File, TaskConfigTypes.Document);
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

	@Override
	public Map<String, Object> getValues() {
		return Map.of(File, base64);
	}
}
