package tom.tasks.transform.htmlformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class TemplateHtmlFormatterConfig implements TaskConfigSpec {

	private String template;

	public TemplateHtmlFormatterConfig() {
		template = "";
	}

	public TemplateHtmlFormatterConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(InlineTemplateHtmlSpecCreator.EnumName)) {
			template = config.get(InlineTemplateHtmlSpecCreator.EnumName).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(InlineTemplateHtmlSpecCreator.EnumName, TaskConfigTypes.EnumList);
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
		return Map.of(InlineTemplateHtmlSpecCreator.EnumName, template);
	}
}
