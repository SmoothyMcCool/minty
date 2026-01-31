package tom.tools.confluence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import tom.api.UserId;
import tom.api.model.services.ConfigurationConsumer;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;
import tom.confluence.ConfluenceClient;
import tom.confluence.model.ChildrenResponse;
import tom.confluence.model.PageResponse;
import tom.confluence.model.SearchRequest;
import tom.confluence.model.SearchResponse;
import tom.tasks.transform.confluence.ConfluenceQueryConfig;

public class ConfluenceTools implements MintyTool, ServiceConsumer, ConfigurationConsumer {

	public static final String MaxPageChracters = "maxPageChars";

	private static final Logger logger = LogManager.getLogger(ConfluenceTools.class);

	private PluginServices pluginServices;
	private String accessToken;
	private boolean useBearerAuth;
	private UserId userId;
	private String confluenceUrl;
	private int maxPageCharacters;
	private ConfluenceClient confluenceClient;

	@Override
	public void initialize() {
		if (pluginServices.getUserService().getUserDefaults(userId).containsKey(ConfluenceQueryConfig.AccessToken)) {
			accessToken = pluginServices.getUserService().getUserDefaults(userId)
					.get(ConfluenceQueryConfig.AccessToken);
		}

		String username = pluginServices.getUserService().getUserDefaults(userId).get(ConfluenceQueryConfig.Username);
		if (username == null) {
			username = "";
		}
		this.confluenceClient = new ConfluenceClient(confluenceUrl, username, accessToken, useBearerAuth,
				maxPageCharacters);
	}

	@Override
	public String name() {
		return "Confluence Tools";
	}

	@Override
	public String description() {
		return "A suite of tools for interacting with Confluence.";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}

	@Override
	public void setProperties(Map<String, String> systemProperties) {
		if (!systemProperties.containsKey(ConfluenceQueryConfig.BaseURL)) {
			throw new PropertyNotFoundException(ConfluenceQueryConfig.BaseURL);
		}
		confluenceUrl = systemProperties.get(ConfluenceQueryConfig.BaseURL);

		if (!systemProperties.containsKey(ConfluenceQueryConfig.UseBearerAuth)) {
			throw new PropertyNotFoundException(ConfluenceQueryConfig.UseBearerAuth);
		}
		useBearerAuth = Boolean.parseBoolean(systemProperties.get(ConfluenceQueryConfig.UseBearerAuth));
	}

	@Override
	public void setPluginConfiguration(Map<String, Object> pluginConfiguration) {
		if (pluginConfiguration.containsKey(MaxPageChracters)) {
			maxPageCharacters = Integer.parseInt(pluginConfiguration.get(MaxPageChracters).toString());
		}
	}

	@Tool(name = "confluence_search_pages", description = "Search Confluence pages by keyword when you do not know the page id. Returns page ids, titles, and snippets.")
	public SearchResponse searchPages(@ToolParam String query, @ToolParam List<String> spaces, @ToolParam int limit) {
		logger.info("confluence_search_pages: {} {} {}", query, spaces, limit);
		return confluenceClient.search(new SearchRequest(query, spaces, limit));
	}

	@Tool(name = "confluence_get_page", description = "Fetch a Confluence page by id, including full text content and metadata.")
	public PageResponse getPage(@ToolParam(description = "The ID of the page to fetch") String pageId) {
		logger.info("confluence_get_page: {}", pageId);
		return confluenceClient.getPage(pageId);
	}

	@Tool(name = "confluence_get_children", description = "List child pages of a Confluence page.")
	public ChildrenResponse getChildren(@ToolParam(description = "The ID of the parent page.") String pageId,
			@ToolParam(description = "The maximum number of child pages to return. Defaults to 10 if not positive.") int limit) {
		logger.info("confluence_get_children: {} {}", pageId, limit);
		return confluenceClient.getChildren(pageId, limit);
	}

	@Tool(name = "confluence_search_by_label", description = "Search Confluence pages by label.")
	public SearchResponse searchByLabel(@ToolParam(description = "The label to search for") String label,
			@ToolParam(description = "Maximum number of results to return") int limit) {
		logger.info("confluence_search_by_label: {} {}", label, limit);
		return confluenceClient.searchByLabel(label, limit);
	}

	@Tool(name = "get_current_local_time", description = "Get the current local time")
	MintyToolResponse<String> getCurrentLocalTime() {
		String result = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
		return MintyToolResponse.SuccessResponse(result);
	}

}