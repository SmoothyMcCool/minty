package tom.tasks.transformer.emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;

public class DataEmitterConfig implements TaskConfigSpec {

	private static final Logger logger = LogManager.getLogger(DataEmitterConfig.class);

	String keyName;
	List<String> data = List.of();

	public DataEmitterConfig() {
	}

	public DataEmitterConfig(Map<String, String> config) {
		keyName = config.get("Key Name");

		String rawData = config.get("Data to Emit");
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

		JsonNode root;
		try {
			root = mapper.readTree(rawData);
			data = new ArrayList<>();
			for (JsonNode node : root) {
				data.add(mapper.writeValueAsString(node));
			}
		} catch (Exception e) {
			logger.warn("Data is not valid JSON list.");
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> config = new HashMap<>();
		config.put("Key Name", TaskConfigTypes.String);
		config.put("Data to Emit", TaskConfigTypes.TextArea);
		return config;
	}

	List<String> getData() {
		return data;
	}

	String getKeyName() {
		return keyName;
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
