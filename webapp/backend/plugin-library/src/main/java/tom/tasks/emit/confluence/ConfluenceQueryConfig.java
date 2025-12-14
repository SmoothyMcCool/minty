package tom.tasks.emit.confluence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.TaskUtils;

public class ConfluenceQueryConfig implements TaskConfigSpec {

	private final String PageIds = "Page IDs";
	private final String Username = "Username";
	private final String AccessToken = "Confluence Access Token";
	private final String BaseURL = "Confluence Base URL";
	private final String UseBearerAuth = "Confluence Use Bearer Authorization";

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
		if (config.containsKey(PageIds)) {
			pages = stringToList(config.get(PageIds));
		}
		if (config.containsKey(Username)) {
			username = config.get(Username);
		}
		if (config.containsKey(AccessToken)) {
			apiKey = config.get(AccessToken);
		}
		if (config.containsKey(BaseURL)) {
			baseUrl = config.get(BaseURL);
		}
		if (config.containsKey(UseBearerAuth)) {
			useBearerAuth = Boolean.getBoolean(config.get(UseBearerAuth));
		}
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put(BaseURL, TaskConfigTypes.String);
		cfg.put(PageIds, TaskConfigTypes.StringList);
		cfg.put(Username, TaskConfigTypes.String);
		cfg.put(AccessToken, TaskConfigTypes.String);
		cfg.put(UseBearerAuth, TaskConfigTypes.Boolean);
		return cfg;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		// return List.of(BaseURL, UseBearerAuth);
		return List.of(UseBearerAuth);
	}

	@Override
	public List<String> getUserConfigVariables() {
		return List.of(Username, AccessToken);
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
			return Map.of(PageIds, new ObjectMapper().writeValueAsString(pages), Username, username, AccessToken,
					apiKey, BaseURL, baseUrl, UseBearerAuth, Boolean.toString(useBearerAuth));
		} catch (JsonProcessingException e) {
			return Map.of(PageIds, "[]", Username, username, AccessToken, apiKey, BaseURL, baseUrl, UseBearerAuth,
					Boolean.toString(useBearerAuth));
		}
	}
}
