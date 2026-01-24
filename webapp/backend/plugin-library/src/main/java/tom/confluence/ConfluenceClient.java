package tom.confluence;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.confluence.model.ChildPage;
import tom.confluence.model.ChildrenResponse;
import tom.confluence.model.PageResponse;
import tom.confluence.model.SearchRequest;
import tom.confluence.model.SearchResponse;
import tom.confluence.model.SearchResult;

public class ConfluenceClient {

	private static final Logger logger = LogManager.getLogger(ConfluenceClient.class);

	private final ObjectMapper mapper;
	private final String baseUrl;
	private final int maxPageChars;
	private final String authHeader;

	public ConfluenceClient(String baseUrl, String username, String accessToken, boolean useBearerAuth,
			int maxPageCharacters) {
		this.mapper = new ObjectMapper();
		this.baseUrl = baseUrl.replaceAll("/+$", "");
		this.maxPageChars = maxPageCharacters;

		if (useBearerAuth) {
			this.authHeader = "Bearer " + accessToken;
		} else {
			String auth = username + ":" + accessToken;
			String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
			this.authHeader = "Basic " + encodedAuth;
		}

	}

	public PageResponse getPage(String pageId) {

		String url = baseUrl + "/rest/api/content/" + pageId + "?expand=body.storage,space,metadata.labels,version";

		try (CloseableHttpClient client = HttpClients.createDefault()) {

			String body = client.execute(newGet(url), okHandler(pageId));
			JsonNode root = mapper.readTree(body);

			String id = root.path("id").asText();
			String title = root.path("title").asText();
			String space = root.path("space").path("key").asText("UNKNOWN");
			String html = root.path("body").path("storage").path("value").asText();
			String webui = root.path("_links").path("webui").asText(null);
			String pageUrl = (webui != null) ? baseUrl + webui : null;
			String lastModified = root.path("version").path("when").asText(null);

			List<String> labels = new ArrayList<>();
			JsonNode labelResults = root.path("metadata").path("labels").path("results");
			if (labelResults.isArray()) {
				for (JsonNode label : labelResults) {
					labels.add(label.path("name").asText());
				}
			}

			return new PageResponse(id, title, space, labels, clean(html), pageUrl, lastModified);

		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch page " + pageId, e);
		}
	}

	public SearchResponse search(SearchRequest request) {

		StringBuilder cql = new StringBuilder("type=page");

		if (request.getQuery() != null && !request.getQuery().isBlank()) {
			String q = request.getQuery().replace("\"", "\\\"");
			cql.append(" AND text ~ \"").append(q).append("\"");
		}

		if (request.getSpaces() != null && !request.getSpaces().isEmpty()) {
			cql.append(" AND space IN (");
			cql.append(String.join(",", request.getSpaces()));
			cql.append(")");
		}

		String url = baseUrl + "/rest/api/content/search" + "?cql=" + encode(cql.toString()) + "&limit="
				+ request.getLimit() + "&expand=space";

		String body;
		JsonNode root;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			body = client.execute(newGet(url), okHandler("search"));
			root = mapper.readTree(body);
		} catch (Exception e) {
			throw new RuntimeException("Confluence search failed", e);
		}

		List<SearchResult> results = new ArrayList<>();

		for (JsonNode r : root.path("results")) {
			results.add(new SearchResult(r.path("id").asText(), r.path("title").asText(),
					r.path("space").path("key").asText("UNKNOWN"), r.path("excerpt").asText("") // may be empty
			));
		}

		return new SearchResponse(results);
	}

	public ChildrenResponse getChildren(String pageId, int limit) {

		String url = baseUrl + "/rest/api/content/" + pageId + "/child/page?limit=" + limit;

		String body;
		JsonNode root;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			body = client.execute(newGet(url), okHandler(pageId));
			root = mapper.readTree(body);
		} catch (Exception e) {
			throw new RuntimeException("Confluence search failed", e);
		}

		List<ChildPage> children = new ArrayList<>();

		for (JsonNode r : root.path("results")) {
			children.add(new ChildPage(r.path("id").asText(), r.path("title").asText()));
		}

		return new ChildrenResponse(children);
	}

	public SearchResponse searchByLabel(String label, int limit) {

		String safeLabel = label.replace("\"", "\\\"");
		String cql = "type=page AND label=\"" + safeLabel + "\"";

		String url = baseUrl + "/rest/api/content/search" + "?cql=" + encode(cql) + "&limit=" + limit + "&expand=space";

		String body;
		JsonNode root;
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			body = client.execute(newGet(url), okHandler(label));
			root = mapper.readTree(body);
		} catch (Exception e) {
			throw new RuntimeException("Confluence search failed", e);
		}
		List<SearchResult> results = new ArrayList<>();

		for (JsonNode r : root.path("results")) {
			results.add(new SearchResult(r.path("id").asText(), r.path("title").asText(),
					r.path("space").path("key").asText("UNKNOWN"), ""));
		}

		return new SearchResponse(results);
	}

	private String clean(String html) {

		if (html == null) {
			return "";
		}

		// Get rid of some confluence macros.
		String cleaned = html.replaceAll("(?s)<ac:[^>]+>.*?</ac:[^>]+>", "").replaceAll("(?s)<ri:[^>]+>.*?</ri:[^>]+>",
				"");

		if (cleaned.length() > maxPageChars) {
			logger.info("Page too long. Truncating to " + maxPageChars + " characters.");
			cleaned = cleaned.substring(0, maxPageChars);
		}

		return cleaned;
	}

	private HttpGet newGet(String url) {
		HttpGet get = new HttpGet(url);
		get.addHeader("Authorization", authHeader);
		get.addHeader("Accept", "application/json");
		return get;
	}

	private HttpClientResponseHandler<String> okHandler(String context) {
		return response -> {
			if (response.getCode() != HttpStatus.SC_OK) {
				throw new RuntimeException("Confluence error (" + context + "): " + response.getCode());
			}
			return EntityUtils.toString(response.getEntity());
		};
	}

	private static String encode(String value) {
		return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
	}
}
