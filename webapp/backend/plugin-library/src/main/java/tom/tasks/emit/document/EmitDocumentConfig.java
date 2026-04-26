package tom.tasks.emit.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;

public class EmitDocumentConfig implements TaskConfigSpec {

	public static final String File = "File";
	public static final String Save = "Save File in Workflow";
	private FileData fileData;
	private boolean save;

	private static final ObjectMapper mapper = new ObjectMapper();

	public EmitDocumentConfig() {
		fileData = new FileData();
	}

	public EmitDocumentConfig(Map<String, Object> config) {
		if (config.containsKey(File)) {
			Object fileField = config.get(File);
			JsonNode node = mapper.valueToTree(fileField);
			String file = node.get("file").get("file").asText();
			String name = node.get("file").get("name").asText();
			fileData = new FileData();
			fileData.setFile(file);
			fileData.setName(name);
			// save = Boolean.getBoolean((String) config.get(Save));
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(File, TaskConfigTypes.Document);
		config.put(Save, TaskConfigTypes.Boolean);
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
		return Map.of(File, fileData, Save, save);
	}
}
