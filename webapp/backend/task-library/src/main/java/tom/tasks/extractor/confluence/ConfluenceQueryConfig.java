package tom.tasks.extractor.confluence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class ConfluenceQueryConfig implements TaskConfig {

	private List<String> pages;
	private String username;
	private String password;
	private String apiKey;
	private String baseUrl;

	public ConfluenceQueryConfig() {
		pages = List.of();
	}

	public ConfluenceQueryConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		pages = stringToList(config.get("Page IDs"));
		username = config.get("Username");
		password = config.get("Password");
		apiKey = config.get("API Key");
		baseUrl = config.get("Base URL");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Base URL", TaskConfigTypes.String);
		cfg.put("Page IDs", TaskConfigTypes.StringList);
		cfg.put("Username", TaskConfigTypes.String);
		cfg.put("Password", TaskConfigTypes.String);
		cfg.put("API Key", TaskConfigTypes.String);
		return cfg;
	}

	public List<String> getPages() {
		return pages;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void updateFrom(String pagesStr) throws JsonMappingException, JsonProcessingException {
		pages = stringToList(pagesStr);
	}

	private List<String> stringToList(String pagesStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(pagesStr, new TypeReference<List<String>>() {
		});
	}
}
