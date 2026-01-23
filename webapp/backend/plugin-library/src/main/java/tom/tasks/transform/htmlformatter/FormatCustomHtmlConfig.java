package tom.tasks.transform.htmlformatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class FormatCustomHtmlConfig implements TaskConfigSpec {

	public static final String PugTemplate = "Pug Template";

	private String template;

	public FormatCustomHtmlConfig() {
		template = "";
	}

	public FormatCustomHtmlConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(PugTemplate)) {
			template = config.get(PugTemplate).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(PugTemplate, TaskConfigTypes.TextArea);
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
		return Map.of(PugTemplate, template);
	}
}
