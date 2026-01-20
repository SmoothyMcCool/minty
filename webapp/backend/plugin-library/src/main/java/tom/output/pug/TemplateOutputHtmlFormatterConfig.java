package tom.output.pug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class TemplateOutputHtmlFormatterConfig implements TaskConfigSpec {

	private String template;

	public TemplateOutputHtmlFormatterConfig() {
		template = "";
	}

	public TemplateOutputHtmlFormatterConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(OutputTemplateHtmlSpecCreator.EnumName)) {
			template = config.get(OutputTemplateHtmlSpecCreator.EnumName).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(OutputTemplateHtmlSpecCreator.EnumName, TaskConfigTypes.EnumList);
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

	@Override
	public Map<String, Object> getValues() {
		return Map.of(OutputTemplateHtmlSpecCreator.EnumName, template);
	}
}
