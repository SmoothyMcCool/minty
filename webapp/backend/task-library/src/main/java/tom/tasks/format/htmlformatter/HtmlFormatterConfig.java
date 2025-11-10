package tom.tasks.format.htmlformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class HtmlFormatterConfig implements TaskConfigSpec {

	private String template = "";

	public HtmlFormatterConfig() {
	}

	public HtmlFormatterConfig(Map<String, String> config) {
		template = config.get("Pug Template");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Pug Template", TaskConfigTypes.EnumList);
		return config;
	}

	public String getTemplate() {
		return template;
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
