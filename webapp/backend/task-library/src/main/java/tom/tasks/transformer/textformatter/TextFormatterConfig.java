package tom.tasks.transformer.textformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class TextFormatterConfig implements TaskConfig {

	String formatStr = "";

	public TextFormatterConfig() {
	}

	public TextFormatterConfig(Map<String, String> config) {
		formatStr = config.get("Format");

		if (formatStr == null || formatStr.isBlank()) {
			return;
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Format", TaskConfigTypes.TextArea);
		return config;
	}

	String getFormat() {
		return formatStr;
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
