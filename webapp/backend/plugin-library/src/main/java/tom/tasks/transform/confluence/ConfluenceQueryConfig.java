package tom.tasks.transform.confluence;

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

	public static final String PageIds = "Page IDs";
	public static final String Username = "Username";
	public static final String AccessToken = "Confluence Access Token";
	public static final String BaseURL = "Confluence Base URL";
	public static final String UseBearerAuth = "Confluence Use Bearer Authorization";
	public static final String MaxPageCharacters = "Maximum Characters to Read from Page";

	private List<String> pages;
	private String username;
	private String apiKey;
	private String baseUrl;
	private Boolean useBearerAuth;
	private int maxPageChars;
	private ConfluencePageConcatenationStrategy concatenationStrategy;

	public ConfluenceQueryConfig() {
		pages = List.of();
		username = "";
		apiKey = "";
		baseUrl = "";
		useBearerAuth = false;
		maxPageChars = 20000;
		concatenationStrategy = ConfluencePageConcatenationStrategy.Concatenated;
	}

	public ConfluenceQueryConfig(Map<String, Object> config) throws JsonMappingException, JsonProcessingException {
		this();
		if (config.containsKey(PageIds)) {
			pages = stringToList(config.get(PageIds).toString());
		}
		if (config.containsKey(Username)) {
			username = config.get(Username).toString();
		}
		if (config.containsKey(AccessToken)) {
			apiKey = config.get(AccessToken).toString();
		}
		if (config.containsKey(BaseURL)) {
			baseUrl = config.get(BaseURL).toString();
		}
		if (config.containsKey(UseBearerAuth)) {
			useBearerAuth = Boolean.parseBoolean(config.get(UseBearerAuth).toString());
		}
		if (config.containsKey(MaxPageCharacters)) {
			maxPageChars = Integer.parseInt(config.get(MaxPageCharacters).toString());
		}
		if (config.containsKey(ConfluenceConcatenationEnumSpecCreator.EnumName)) {
			concatenationStrategy = ConfluencePageConcatenationStrategy
					.valueOf(config.get(ConfluenceConcatenationEnumSpecCreator.EnumName).toString());
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
		cfg.put(MaxPageCharacters, TaskConfigTypes.Number);
		cfg.put(ConfluenceConcatenationEnumSpecCreator.EnumName, TaskConfigTypes.EnumList);
		return cfg;
	}

	@Override
	public List<String> getSystemConfigVariables() {
		return List.of(BaseURL, UseBearerAuth);
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

	public int getMaxPageCharacters() {
		return maxPageChars;
	}

	public ConfluencePageConcatenationStrategy getConcatenationStrategy() {
		return concatenationStrategy;
	}

	public void updateFrom(Map<String, Object> obj) throws JsonMappingException, JsonProcessingException {
		if (obj.containsKey(PageIds)) {
			pages.addAll(TaskUtils.safeConvert(obj.get(PageIds), new TypeReference<List<String>>() {
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
	public Map<String, Object> getValues() {
		try {
			return Map.of(PageIds, new ObjectMapper().writeValueAsString(pages), Username, username, AccessToken,
					apiKey, BaseURL, baseUrl, UseBearerAuth, Boolean.toString(useBearerAuth),
					ConfluenceConcatenationEnumSpecCreator.EnumName, concatenationStrategy);
		} catch (JsonProcessingException e) {
			return Map.of(PageIds, "[]", Username, username, AccessToken, apiKey, BaseURL, baseUrl, UseBearerAuth,
					Boolean.toString(useBearerAuth), ConfluenceConcatenationEnumSpecCreator.EnumName,
					concatenationStrategy);
		}
	}
}
