package tom.tasks.transform.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class PythonExecutorConfig implements TaskConfigSpec {

	private final String PythonCode = "Python Code";

	private String python;

	public PythonExecutorConfig() {
		python = "";
	}

	public PythonExecutorConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		this();
		if (config.containsKey(PythonCode)) {
			python = config.get(PythonCode);
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(PythonCode, TaskConfigTypes.TextArea);
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

	@Override
	public Map<String, String> getValues() {
		return Map.of(PythonCode, python);
	}
}
