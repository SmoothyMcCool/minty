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

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.services.assistant.AssistantManagementService;
import tom.config.MintyConfiguration;

@Service
public class AssistantRegistry {

	private static final Logger logger = LogManager.getLogger(AssistantRegistry.class);
	private static ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	private final Map<String, Assistant> helpers;
	private final Map<String, Assistant> orchestrators;
	private final Map<String, Assistant> workers;

	public AssistantRegistry(MintyConfiguration config) throws StreamReadException, DatabindException, IOException {
		helpers = new HashMap<>();
		orchestrators = new HashMap<>();
		workers = new HashMap<>();

		Path agentRoot = config.getConfig().fileStores().agents();
		Path helpersPath = agentRoot.resolve("helpers");
		Path orchestratorsPath = agentRoot.resolve("orchestrators");
		Path workersPath = agentRoot.resolve("workers");

		try (Stream<Path> helpersStream = Files.list(helpersPath);
				Stream<Path> orchestratorsStream = Files.list(orchestratorsPath);
				Stream<Path> workersStream = Files.list(workersPath)) {

			helpersStream.filter(Files::isRegularFile).forEach((p) -> {
				Assistant assistant = buildAssistant(p);
				if (assistant != null) {
					helpers.put(assistant.name(), assistant);
					logger.info("Added helper " + assistant.name());
				}
			});
			orchestratorsStream.filter(Files::isRegularFile).forEach((p) -> {
				Assistant assistant = buildAssistant(p);
				if (assistant != null) {
					orchestrators.put(assistant.name(), assistant);
					logger.info("Added orchestrator " + assistant.name());
				}
			});
			workersStream.filter(Files::isRegularFile).forEach((p) -> {
				Assistant assistant = buildAssistant(p);
				if (assistant != null) {
					workers.put(assistant.name(), assistant);
					logger.info("Added worker " + assistant.name());
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
		} catch (IOException e) {
			logger.warn("Could not read agent file " + assistantFilePath.getFileName());
		}
		return null;
	}

	public Assistant getWorker(String name) {
		return new AssistantBuilder(workers.get(name)).build();
	}

	public boolean hasWorker(String key) {
		return workers.containsKey(key);
	}

	public Assistant getOrchestrator(String name) {
		return new AssistantBuilder(orchestrators.get(name)).build();
	}

	public boolean hasOrchestrator(String key) {
		return orchestrators.containsKey(key);
	}

	public Assistant getHelper(String name) {
		return new AssistantBuilder(helpers.get(name)).build();
	}

	public boolean hasHelper(String key) {
		return helpers.containsKey(key);
	}
}
