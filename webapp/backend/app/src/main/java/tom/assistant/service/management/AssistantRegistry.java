package tom.assistant.service.management;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tom.api.model.Assistant;
import tom.api.model.AssistantBuilder;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;

public class AssistantRegistry {

	private final Map<String, Assistant> assistants;

	@SuppressWarnings("unchecked")
	public AssistantRegistry() throws StreamReadException, DatabindException, IOException {
		assistants = new HashMap<>();
		String path = System.getProperty("catalina.base") + "/conf/Minty/assistants.yaml";

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		Map<String, Object> data = mapper.readValue(new File(path), Map.class);

		List<Object> rawAssistants = (List<Object>) data.get("assistants");

		for (Object asstObj : rawAssistants) {
			Map<String, Object> asstData = (Map<String, Object>) asstObj;

			AssistantBuilder builder = new AssistantBuilder();

			List<String> tools = mapper.convertValue(asstData.get("tools"), new TypeReference<List<String>>() {
			});

			builder.id(AssistantManagementService.DefaultAssistantId).name((String) asstData.get("name"))
					.model((String) asstData.get("model")).temperature((Double) asstData.get("temperature"))
					.topK((Integer) asstData.get("topK")).prompt((String) asstData.get("prompt")).tools(tools)
					.ownerId(UserService.DefaultId).shared((Boolean) asstData.get("shared"))
					.hasMemory((Boolean) asstData.get("hasMemory")).documentIds(List.of());
			Assistant assistant = builder.build();
			assistants.put(assistant.name(), assistant);
		}
	}

	public Assistant get(String name, String model) {
		return new AssistantBuilder(assistants.get(name)).model(model).build();
	}

	public boolean has(String key) {
		return assistants.containsKey(key);
	}
}
