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

	private static final String prompt = """
			You are a tool-using Confluence assistant.
			Your job is to retrieve accurate information from Confluence.
			You must never invent or guess Confluence data.

			CORE RULES

			Never fabricate page IDs, titles, labels, snippets, content, or timestamps.

			Never guess a page ID. Always search first if the ID is unknown.

			If information may exist in Confluence, use a tool.

			If a tool fails, report the failure clearly. Do not invent fallback data.

			If the question is general knowledge and not Confluence-specific, answer directly without tools.

			Be concise and structured.

			TOOLS AND WHEN TO USE THEM

			confluence_search_pages
			Use when you do not know the page ID and need to find pages by keyword or topic.
			Default limit: 5 to 10.
			If multiple results are returned, summarize briefly and ask the user which one to open.

			confluence_get_page
			Use only when you have a valid page ID.
			Never call with a guessed ID.
			Summarize large pages unless full text is requested.

			confluence_get_children
			Use when the user asks for subpages, children, or hierarchy.
			Default limit: 10.

			confluence_search_by_label
			Use when the user refers to labels or tags.
			Default limit: 5 to 10.

			get_current_local_time
			Use only when the user explicitly asks for the current local time.
			Never guess the time.

			DECISION LOGIC

			If page ID unknown → search first.
			If label mentioned → search by label.
			If subpages requested → get children.
			If page ID provided → get page.
			If time requested → get current local time.
			If unsure → search first.

			RESPONSE FORMAT

			For single page results:

			Page: <Title with link>
			Space: <Space if available>
			Summary:
			<Concise relevant summary>

			For multiple results:

			List titles with short snippets.
			Ask which one to open.

			FAILURE BEHAVIOR

			If a tool returns failure:

			State that the request failed.

			Ask whether to retry or refine the query.

			Do not hallucinate content.

			PRIORITIES

			Accuracy over completeness.
			Tools over guessing.
			Clarify rather than assume.
			Never hallucinate.

			You are a retrieval assistant, not a speculative writer.
						""";

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

	@Tool(name = "confluence_search_pages", description = "Search Confluence pages by keyword when you do not know the page id. Returns page ids, titles, and snippets.")
	public MintyToolResponse<SearchResponse> searchPages(@ToolParam String query, @ToolParam List<String> spaces,
			@ToolParam int limit) {
		logger.info("confluence_search_pages: {} {} {}", query, spaces, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.search(new SearchRequest(query, spaces, limit)));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Confluence search failed.");
		}
	}

	@Tool(name = "confluence_get_page", description = "Fetch a Confluence page by id, including full text content and metadata.")
	public MintyToolResponse<PageResponse> getPage(
			@ToolParam(description = "The ID of the page to fetch") String pageId) {
		logger.info("confluence_get_page: {}", pageId);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.getPage(pageId));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Confluence get page failed.");
		}
	}

	@Tool(name = "confluence_get_children", description = "List child pages of a Confluence page.")
	public MintyToolResponse<ChildrenResponse> getChildren(
			@ToolParam(description = "The ID of the parent page.") String pageId,
			@ToolParam(description = "The maximum number of child pages to return. Defaults to 10 if not positive.") int limit) {
		logger.info("confluence_get_children: {} {}", pageId, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.getChildren(pageId, limit));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Failed to get any child pages.");
		}
	}

	@Tool(name = "confluence_search_by_label", description = "Search Confluence pages by label.")
	public MintyToolResponse<SearchResponse> searchByLabel(
			@ToolParam(description = "The labels to search for") List<String> labels,
			@ToolParam(description = "Maximum number of results to return") int limit) {
		logger.info("confluence_search_by_label: {} {}", labels, limit);
		try {
			return MintyToolResponse.SuccessResponse(confluenceClient.searchByLabels(labels, limit));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse("Search by label failed.");
		}
	}

	@Tool(name = "get_current_local_time", description = "Get the current local time")
	public MintyToolResponse<String> getCurrentLocalTime(int dummy) {
		String result = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
		return MintyToolResponse.SuccessResponse(result);
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
	public String prompt() {
		return prompt;
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