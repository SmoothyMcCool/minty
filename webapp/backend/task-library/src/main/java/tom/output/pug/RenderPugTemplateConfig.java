package tom.output.pug;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class RenderPugTemplateConfig implements TaskConfigSpec {

	private String template;

	public RenderPugTemplateConfig() {
		template = "";
	}

	public RenderPugTemplateConfig(Map<String, String> config) {
		this();
		if (config.containsKey("Pug Template")) {
			template = config.get("Pug Template");
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Pug Template", TaskConfigTypes.TextArea);
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
		return Map.of("Pug Template", template);
	}
}
