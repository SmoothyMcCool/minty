package tom.tasks.transform.confluence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import tom.api.MintyObjectMapper;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskConfigTypes;
import tom.tasks.TaskUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

public class ConfluenceQueryConfig implements TaskConfigSpec {

	public static final String PageIds = "Page IDs";
	public static final String Username = "Username";
	public static final String AccessToken = "Confluence Access Token";
	public static final String BaseURL = "Confluence Base URL";
	public static final String UseBearerAuth = "Confluence Use Bearer Authorization";
	public static final String MaxPageCharacters = "Maximum Characters to Read from Page";

	private static final ObjectMapper Mapper = MintyObjectMapper.StandardJsonMapper;

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

	public ConfluenceQueryConfig(Map<String, Object> config) throws DatabindException, JacksonException {
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

	public void updateFrom(Map<String, Object> obj) throws DatabindException, JacksonException {
		if (obj.containsKey(PageIds)) {
			pages.addAll(TaskUtils.safeConvert(obj.get(PageIds), new TypeReference<List<String>>() {
			}));
		}
	}

	private List<String> stringToList(String pagesStr) throws DatabindException, JacksonException {
		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;
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
			return Map.of(PageIds, Mapper.writeValueAsString(pages), Username, username, AccessToken, apiKey, BaseURL,
					baseUrl, UseBearerAuth, Boolean.toString(useBearerAuth), MaxPageCharacters, maxPageChars,
					ConfluenceConcatenationEnumSpecCreator.EnumName, concatenationStrategy);
		} catch (JacksonException e) {
			return Map.of(PageIds, "[]", Username, username, AccessToken, apiKey, BaseURL, baseUrl, UseBearerAuth,
					Boolean.toString(useBearerAuth), MaxPageCharacters, maxPageChars,
					ConfluenceConcatenationEnumSpecCreator.EnumName, concatenationStrategy);
		}
	}
}
