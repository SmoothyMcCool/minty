package tom.output.pug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class RenderPugTemplateConfig implements TaskConfigSpec {

	private final String PugTemplate = "Pug Template";

	private String template;

	public RenderPugTemplateConfig() {
		template = "";
	}

	public RenderPugTemplateConfig(Map<String, String> config) {
		this();
		if (config.containsKey(PugTemplate)) {
			template = config.get(PugTemplate);
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
	public Map<String, String> getValues() {
		return Map.of(PugTemplate, template);
	}
}
