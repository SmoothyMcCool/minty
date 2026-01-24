package tom.config.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public record MintyConfig(OutputConfig output, SessionConfig session, FileStoresConfig fileStores, DatabaseConfig db,
		OllamaConfig ollama, ThreadPoolConfig threads, String secret, List<PluginConfig> pluginConfiguration,
		List<String> userDefaults, Map<String, Object> systemDefaults) {

	public String prettyPrint() {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.enable(SerializationFeature.INDENT_OUTPUT);

			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return toString();
		}
	}
}
