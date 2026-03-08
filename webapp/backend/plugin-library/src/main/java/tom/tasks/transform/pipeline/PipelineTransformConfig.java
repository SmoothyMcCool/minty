package tom.tasks.transform.pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.transform.pipeline.model.PipelineDefinition;
import tom.tasks.transform.pipeline.model.PipelineOperationConfiguration;
import tom.tasks.transform.pipeline.model.PipelineOperationConfigurationDeserializer;

public class PipelineTransformConfig implements TaskConfigSpec {

	public static final String Definition = "Definition";

	private static final ObjectMapper mapper = createMapper();

	private static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule module = new SimpleModule();
		module.addDeserializer(PipelineOperationConfiguration.class, new PipelineOperationConfigurationDeserializer());
		mapper.registerModule(module);
		return mapper;
	}

	private String definition;

	public PipelineTransformConfig() {
		definition = "";
	}

	public PipelineTransformConfig(Map<String, Object> config) {
		this();
		if (config.containsKey(Definition)) {
			definition = config.get(Definition).toString();
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put(Definition, TaskConfigTypes.PipelineEditor);
		return config;
	}

	public String getDefinition() {
		return definition;
	}

	public PipelineDefinition getPipelineDefinition() {
		try {
			return mapper.readValue(definition, PipelineDefinition.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to deserialize pipeline definition", e);
		}
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
		return Map.of(Definition, definition);
	}
}
