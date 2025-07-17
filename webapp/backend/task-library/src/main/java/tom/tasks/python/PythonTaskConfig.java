package tom.tasks.python;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.AiTaskConfig;
import tom.task.AiTaskConfigTypes;

public class PythonTaskConfig implements AiTaskConfig {

    private String pythonFile;
    private Map<String, String> inputDictionary;

    public PythonTaskConfig() {
    }

    @SuppressWarnings("unchecked")
    public PythonTaskConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        pythonFile = config.get("Python File");
        inputDictionary = objectMapper.readValue(config.get("InputDictionary"), Map.class);
    }

    @Override
    public Map<String, AiTaskConfigTypes> getConfig() {
        Map<String, AiTaskConfigTypes> config = new HashMap<>();
        config.put("Python File", AiTaskConfigTypes.String);
        config.put("InputDictionary", AiTaskConfigTypes.String);
        return config;
    }

    public String getPythonFile() {
        return pythonFile;
    }

    public Map<String, String> getInputDictionary() {
        return inputDictionary;
    }

}
