package tom.tasks.transform.formattext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class FormatTextConfig implements TaskConfigSpec {

	public static final String Format = "Format";

	private String formatStr;

	public FormatTextConfig() {
		formatStr = "";
	}

	public FormatTextConfig(Map<String, Object> config) {
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
