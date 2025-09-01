package tom.output.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class JsonOutputConfig implements TaskConfig {

	public JsonOutputConfig() {
	}

	public JsonOutputConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		return config;
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
