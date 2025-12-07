package tom.tasks.emit.confluence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.TaskConfigSpec;
import tom.task.TaskConfigTypes;
import tom.tasks.TaskUtils;

public class ConfluenceQueryConfig implements TaskConfigSpec {

	private List<String> pages;
	private String username;
	private String apiKey;
	private String baseUrl;
	private Boolean useBearerAuth;

	public ConfluenceQueryConfig() {
		pages = List.of();
		username = "";
		apiKey = "";
		baseUrl = "";
		useBearerAuth = false;
	}

	public ConfluenceQueryConfig(Map<String, String> config) throws JsonMappingException, JsonProcessingException {
		this();
		if (config.containsKey("Page IDs")) {
			pages = stringToList(config.get("Page IDs"));
		}
		if (config.containsKey("Username")) {
			username = config.get("Username");
		}
		if (config.containsKey("Access Token")) {
			apiKey = config.get("Access Token");
		}
		if (config.containsKey("Base URL")) {
			baseUrl = config.get("Base URL");
		}
		if (config.containsKey("Use Bearer Authorization")) {
			useBearerAuth = Boolean.getBoolean(config.get("Use Bearer Authorization"));
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Base URL", TaskConfigTypes.String);
		cfg.put("Page IDs", TaskConfigTypes.StringList);
		cfg.put("Username", TaskConfigTypes.String);
		cfg.put("Access Token", TaskConfigTypes.String);
		cfg.put("Use Bearer Authorization", TaskConfigTypes.Boolean);
		return cfg;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of("Base URL", "Use Bearer Authorization");
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of("Username", "Access Token");
	}

	public List<String> getPages() {
		return pages;
	}

	public String getUsername() {
		return username;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public Boolean getUseBearerAuth() {
		return useBearerAuth;
	}

	public void updateFrom(Map<String, Object> obj) throws JsonMappingException, JsonProcessingException {
		if (obj.containsKey("Pages")) {
			pages.addAll(TaskUtils.safeConvert(obj.get("Pages"), new TypeReference<List<String>>() {
			}));
		}
	}

	private List<String> stringToList(String pagesStr) throws JsonMappingException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(pagesStr, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			throw new StringListFormatException("Could not read list of page IDs", e);
		}

	}

	@Override
	public Map<String, String> getValues() {
		try {
			return Map.of("Page IDs", new ObjectMapper().writeValueAsString(pages), "Username", username,
					"Access Token", apiKey, "Base URL", baseUrl, "Use Bearer Authorization",
					Boolean.toString(useBearerAuth));
		} catch (JsonProcessingException e) {
			return Map.of("Page IDs", "[]", "Username", username, "Access Token", apiKey, "Base URL", baseUrl,
					"Use Bearer Authorization", Boolean.toString(useBearerAuth));
		}
	}
}
