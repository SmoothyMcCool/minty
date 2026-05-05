package tom.assistant.service.management;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import tom.api.MintyObjectMapper;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.services.assistant.AssistantManagementService;
import tom.config.MintyConfiguration;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

@Service
public class AssistantRegistry {

	private static final Logger logger = LogManager.getLogger(AssistantRegistry.class);
	private static ObjectMapper mapper = MintyObjectMapper.StandardYamlMapper;

	private final Map<String, Assistant> assistants;

	public AssistantRegistry(MintyConfiguration config) throws StreamReadException, DatabindException, IOException {
		assistants = new HashMap<>();

		Path agentRoot = config.getConfig().fileStores().agents();
		Path helpersPath = agentRoot.resolve("assistants");

		try (Stream<Path> helpersStream = Files.list(helpersPath)) {

			helpersStream.filter(Files::isRegularFile).forEach((p) -> {
				Assistant assistant = buildAssistant(p);
				if (assistant != null) {
					assistants.put(assistant.name(), assistant);
					logger.info("Added assistants " + assistant.name());
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@SuppressWarnings("unchecked")
	private Assistant buildAssistant(Path assistantFilePath) {
		Map<String, Object> data;
		try {
			data = mapper.readValue(assistantFilePath.toFile(), Map.class);
			AssistantBuilder builder = new AssistantBuilder();

			List<String> tools = mapper.convertValue(data.get("tools"), new TypeReference<List<String>>() {
			});

			builder.id(AssistantManagementService.DefaultAssistantId).name((String) data.get("name"))
					.model((String) data.get("model")).contextSize((Integer) data.get("contextSize"))
					.temperature((Double) data.get("temperature")).topK((Integer) data.get("topK"))
					.prompt((String) data.get("prompt")).tools(tools).owned(false) // Assistants from the registry
																					// are never owned by a user.
					.hasMemory((Boolean) data.get("hasMemory")).documentIds(List.of());
			return builder.build();
		} catch (IllegalArgumentException e) {
			logger.warn("Could not read agent file " + assistantFilePath.getFileName());
		}
		return null;
	}

	public Assistant getAssistant(String name) {
		return new AssistantBuilder(assistants.get(name)).build();
	}

	public boolean hasAssistant(String key) {
		return assistants.containsKey(key);
	}

}
