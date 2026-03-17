package tom.tasks.emit.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class EmitDocumentConfig implements TaskConfigSpec {

	public static final String File = "File";
	private FileData fileData;

	private static final ObjectMapper mapper = new ObjectMapper();

	public EmitDocumentConfig() {
		fileData = new FileData();
	}

	public EmitDocumentConfig(Map<String, Object> config) {
		if (config.containsKey(File)) {
			try {
				fileData = mapper.readValue(mapper.writeValueAsString(config.get(File)), FileData.class);
			} catch (JsonProcessingException e) {
				fileData = null;
			}
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(File, TaskConfigTypes.Document);
		return config;
	}

	FileData getFileData() {
		return fileData;
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
	public Map<String, Object> getValues() {
		return Map.of(File, fileData);
	}
}
