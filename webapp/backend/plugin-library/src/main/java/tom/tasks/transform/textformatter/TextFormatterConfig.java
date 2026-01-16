package tom.tasks.transform.textformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class TextFormatterConfig implements TaskConfigSpec {

	public static final String Format = "Format";

	private String formatStr;

	public TextFormatterConfig() {
		formatStr = "";
	}

	public TextFormatterConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(Format)) {
			formatStr = config.get(Format).toString();
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
	public Map<String, Object> getValues() {
		return Map.of(Format, formatStr);
	}

}
