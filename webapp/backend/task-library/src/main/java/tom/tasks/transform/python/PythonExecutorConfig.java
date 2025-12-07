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
		python = "";
	}

	public PythonExecutorConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		this();
		if (config.containsKey("Python Code")) {
			python = config.get("Python Code");
		}
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

	@Override
	public Map<String, String> getValues() {
		return Map.of("Python Code", python);
	}
}
