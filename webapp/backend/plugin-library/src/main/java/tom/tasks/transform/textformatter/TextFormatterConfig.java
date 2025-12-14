package tom.tasks.transform.textformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class TextFormatterConfig implements TaskConfigSpec {

	private final String Format = "Format";

	private String formatStr;

	public TextFormatterConfig() {
		formatStr = "";
	}

	public TextFormatterConfig(Map<String, String> config) {
		this();
		if (config.containsKey(Format)) {
			formatStr = config.get(Format);
		}

		if (formatStr == null || formatStr.isBlank()) {
			return;
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(Format, TaskConfigTypes.TextArea);
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

	@Override
	public Map<String, String> getValues() {
		return Map.of(Format, formatStr);
	}

}
