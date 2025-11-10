package tom.tasks.transform.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class PythonExecutorConfig implements TaskConfigSpec {

	private String python;

	public PythonExecutorConfig() {
	}

	public PythonExecutorConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		python = config.get("Python Code");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Python Code", TaskConfigTypes.TextArea);
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
