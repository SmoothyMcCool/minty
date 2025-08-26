package tom.output.pug;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class RenderPugTemplateConfig implements TaskConfig {

	private String template = "";

	public RenderPugTemplateConfig() {
	}

	public RenderPugTemplateConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		template = config.get("Pug Template");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Pug Template", TaskConfigTypes.String);
		return config;
	}

	public String getTemplate() {
		return template;
	}

}
