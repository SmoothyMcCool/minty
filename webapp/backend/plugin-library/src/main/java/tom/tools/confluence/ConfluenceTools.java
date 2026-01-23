package tom.tools.confluence;

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
import tom.tasks.transform.confluence.ConfluenceClient;
import tom.tasks.transform.confluence.ConfluenceQueryConfig;
import tom.tasks.transform.confluence.model.ChildrenRequest;
import tom.tasks.transform.confluence.model.ChildrenResponse;
import tom.tasks.transform.confluence.model.LabelSearchRequest;
import tom.tasks.transform.confluence.model.PageRequest;
import tom.tasks.transform.confluence.model.PageResponse;
import tom.tasks.transform.confluence.model.SearchRequest;
import tom.tasks.transform.confluence.model.SearchResponse;

public class ConfluenceTools implements MintyTool, ServiceConsumer, ConfigurationConsumer {

	private static final Logger logger = LogManager.getLogger(ConfluenceTools.class);

	private PluginServices pluginServices;
	private String accessToken;
	private boolean useBearerAuth;
	private UserId userId;
	private String confluenceUrl;
	private ConfluenceClient confluenceClient;

	@Override
	public void initialize() {
		String username = pluginServices.getUserService().getUsernameFromId(userId);
		this.confluenceClient = new ConfluenceClient(confluenceUrl, username, accessToken, useBearerAuth);
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
	public void setProperties(Map<String, String> systemProperties, Map<String, String> userProperties) {
		if (!systemProperties.containsKey(ConfluenceQueryConfig.BaseURL)) {
			throw new PropertyNotFoundException(ConfluenceQueryConfig.BaseURL);
		}
		confluenceUrl = systemProperties.get(ConfluenceQueryConfig.BaseURL);

		if (!systemProperties.containsKey(ConfluenceQueryConfig.UseBearerAuth)) {
			throw new PropertyNotFoundException(ConfluenceQueryConfig.UseBearerAuth);
		}
		useBearerAuth = Boolean.parseBoolean(systemProperties.get(ConfluenceQueryConfig.UseBearerAuth));

		if (!userProperties.containsKey(ConfluenceQueryConfig.AccessToken)) {
			throw new PropertyNotFoundException(ConfluenceQueryConfig.AccessToken);
		}
		accessToken = userProperties.get(ConfluenceQueryConfig.AccessToken);
	}

	@Tool(name = "confluence_search_pages", description = "Search Confluence pages by keyword when you do not know the page id. Returns page ids, titles, and snippets.")
	public SearchResponse searchPages(@ToolParam SearchRequest request) {
		logger.debug("confluence_search_pages: {}", request);
		return confluenceClient.search(request);
	}

	@Tool(name = "confluence_get_page", description = "Fetch a Confluence page by id, including full text content and metadata.")
	public PageResponse getPage(@ToolParam PageRequest request) {
		logger.debug("confluence_get_page: {}", request);
		return confluenceClient.getPage(request.getPageId());
	}

	@Tool(name = "confluence_get_children", description = "List child pages of a Confluence page.")
	public ChildrenResponse getChildren(@ToolParam ChildrenRequest request) {
		logger.debug("confluence_get_children: {}", request);
		return confluenceClient.getChildren(request.getPageId(), request.getLimit());
	}

	@Tool(name = "confluence_search_by_label", description = "Search Confluence pages by label.")
	public SearchResponse searchByLabel(@ToolParam LabelSearchRequest request) {
		logger.debug("confluence_search_by_label: {}", request);
		return confluenceClient.searchByLabel(request.getLabel(), request.getLimit());
	}

}