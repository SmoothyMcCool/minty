package tom.tasks.emit.text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class TextEmitterConfig implements TaskConfigSpec {

	public static final String TextToEmit = "Text";

	String text;

	public TextEmitterConfig() {
		text = "";
	}

	public TextEmitterConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(TextToEmit)) {
			text = config.get(TextToEmit).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(TextToEmit, TaskConfigTypes.TextArea);
		return config;
	}

	String getText() {
		return text;
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
		return Map.of(TextToEmit, text);
	}
}
