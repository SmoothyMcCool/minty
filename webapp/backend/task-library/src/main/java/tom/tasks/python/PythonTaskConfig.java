package tom.tasks.python;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class PythonTaskConfig implements TaskConfig {

	private String pythonFile;
	private Map<String, Object> inputDictionary = new HashMap<>();

	public PythonTaskConfig() {
	}

	@SuppressWarnings("unchecked")
	public PythonTaskConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper();
		pythonFile = config.get("Python File");
		inputDictionary = objectMapper.readValue(config.get("InputDictionary"), Map.class);
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Python File", TaskConfigTypes.String);
		config.put("InputDictionary", TaskConfigTypes.String);
		return config;
	}

	public String getPythonFile() {
		return pythonFile;
	}

	public Map<String, Object> getInputDictionary() {
		return inputDictionary;
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
