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
import tom.tasks.TaskUtils;

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
		apiKey = config.get("Access Token");
		baseUrl = config.get("Base URL");
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Base URL", TaskConfigTypes.String);
		cfg.put("Page IDs", TaskConfigTypes.StringList);
		cfg.put("Username", TaskConfigTypes.String);
		cfg.put("Access Token", TaskConfigTypes.String);
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

	public void updateFrom(Map<String, Object> obj) throws JsonMappingException, JsonProcessingException {
		if (obj.containsKey("Data")) {
			pages = TaskUtils.safeConvert(obj.containsKey("Data"), new TypeReference<List<String>>() {
			});
		}
	}

	private List<String> stringToList(String pagesStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(pagesStr, new TypeReference<List<String>>() {
		});
	}
}
