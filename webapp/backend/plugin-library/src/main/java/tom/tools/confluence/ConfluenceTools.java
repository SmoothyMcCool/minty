package tom.tools.confluence;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

	// ---------------------------------------------------------------------
	// INITIALIZATION
	// ---------------------------------------------------------------------

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
				maxPageCharacters, pluginServices.getCacheService().getCache("confluenceCache"));
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

	// ---------------------------------------------------------------------
	// SEARCH PAGES
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_search_pages", description = """
			Search Confluence pages by keyword.

			Returns:
			- page id
			- title
			- snippet
			""")
	public MintyToolResponse<SearchResponse> searchPages(@ToolParam(description = "Search query") String query,
			@ToolParam(description = "Spaces to search in") List<String> spaces,
			@ToolParam(description = "Max results") int limit) {
		logger.info("confluence_search_pages: {} {} {}", query, spaces, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.search(new SearchRequest(query, spaces, limit)));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Search failed.");
		}
	}

	// ---------------------------------------------------------------------
	// GET PAGE
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_get_page", description = """
			Get a Confluence page by id.

			Returns:
			- full page content
			- metadata
			""")
	public MintyToolResponse<PageResponse> getPage(@ToolParam(description = "Page id") String pageId) {
		logger.info("confluence_get_page: {}", pageId);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.getPage(pageId));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Failed to get page.");
		}
	}

	// ---------------------------------------------------------------------
	// GET PAGES (BATCH)
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_get_pages", description = """
			Get multiple Confluence pages by id.

			Use this instead of repeated single-page calls.
			""")
	public MintyToolResponse<List<PageResponse>> getPages(@ToolParam(description = "Page ids") List<String> pageIds) {
		logger.info("confluence_get_pages: {}", pageIds);
		try {
			List<PageResponse> pages = pageIds.stream().map(id -> {
				try {
					return confluenceClient.getPage(id);
				} catch (Exception e) {
					logger.warn("failed page fetch {}", id);
					return null;
				}
			}).filter(Objects::nonNull).toList();
			return MintyToolResponse.SuccessResponse(pages);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Failed to get pages.");
		}
	}

	// ---------------------------------------------------------------------
	// GET CHILDREN
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_get_children", description = "Get child pages of a Confluence page.")
	public MintyToolResponse<ChildrenResponse> getChildren(@ToolParam(description = "Parent page id") String pageId,
			@ToolParam(description = "Max results") int limit) {
		logger.info("confluence_get_children: {} {}", pageId, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.getChildren(pageId, limit));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Failed to get children.");
		}
	}

	// ---------------------------------------------------------------------
	// SEARCH BY LABEL
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_search_by_label", description = "Search pages by label.")
	public MintyToolResponse<SearchResponse> searchByLabel(@ToolParam(description = "Labels") List<String> labels,
			@ToolParam(description = "Max results") int limit) {
		logger.info("confluence_search_by_label: {} {}", labels, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.searchByLabels(labels, limit));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Label search failed.");
		}
	}

	// ---------------------------------------------------------------------
	// TIME TOOL
	// ---------------------------------------------------------------------

	@Tool(name = "confluence_get_current_time", description = "Get current local time")
	public MintyToolResponse<String> getCurrentLocalTime(int ignored) {
		String result = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
		return MintyToolResponse.SuccessResponse(result);
	}

	// ---------------------------------------------------------------------

	@Override
	public String name() {
		return "Confluence Tools";
	}

	@Override
	public String description() {
		return "Tools for interacting with Confluence.";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}
}