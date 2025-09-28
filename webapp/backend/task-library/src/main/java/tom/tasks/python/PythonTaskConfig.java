package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class PythonTaskConfig implements TaskConfig {

	private String python;

	public PythonTaskConfig() {
	}

	public PythonTaskConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		python = config.get("Python");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Python", TaskConfigTypes.TextArea);
		return config;
	}

	public String getPython() {
		return python;
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
