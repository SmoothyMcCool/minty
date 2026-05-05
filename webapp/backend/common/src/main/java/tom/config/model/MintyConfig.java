package tom.config.model;

import java.util.List;
import java.util.Map;

import tom.api.MintyObjectMapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public record MintyConfig(OutputConfig output, SessionConfig session, FileStoresConfig fileStores, DatabaseConfig db,
		LlmConfig llm, PandocConfig pandoc, ThreadPoolConfig threads, String secret,
		List<PluginConfig> pluginConfiguration, List<String> userDefaults, Map<String, Object> systemDefaults) {

	public String prettyPrint() {
		try {
			ObjectMapper mapper = MintyObjectMapper.PrettyPrinterJsonMapper;

			return mapper.writeValueAsString(this);
		} catch (JacksonException e) {
			return toString();
		}
	}
}
