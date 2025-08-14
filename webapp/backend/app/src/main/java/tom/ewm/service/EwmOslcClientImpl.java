package tom.ewm.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.ewm.EwmOslcClient;
import tom.api.services.ewm.ResourceRef;
import tom.api.services.ewm.WorkItem;

public class EwmOslcClientImpl implements EwmOslcClient {

	private final String server;
	private final String username;
	private final String password;
	private String cookies;

	private final ObjectMapper mapper = new ObjectMapper();

	public EwmOslcClientImpl(String server, String username, String password) {
		this.server = server.endsWith("/ccm") ? server : server + "/ccm";
		this.username = username;
		this.password = password;
	}

	@Override
	public void login() throws IOException {
		String loginUrl = server + "/j_security_check";
		String body = "j_username=" + URLEncoder.encode(username, "UTF-8") + "&j_password="
				+ URLEncoder.encode(password, "UTF-8");

		HttpURLConnection conn = (HttpURLConnection) URI.create(loginUrl).toURL().openConnection();
		conn.setInstanceFollowRedirects(false); // don't auto-follow
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		try (OutputStream os = conn.getOutputStream()) {
			os.write(body.getBytes(StandardCharsets.UTF_8));
		}

		List<String> setCookies = conn.getHeaderFields().get("Set-Cookie");
		if (setCookies == null || setCookies.isEmpty()) {
			throw new RuntimeException("Login failed: no Set-Cookie returned (check credentials and auth config)");
		}
		cookies = String.join("; ", setCookies);
		conn.disconnect();

		// Optional: sanity check we can access a protected resource
		HttpURLConnection check = openGet(server + "/rootservices", "application/xml");
		int code = check.getResponseCode();
		if (code >= 400) {
			throw new RuntimeException("Login check failed: HTTP " + code);
		}
		check.disconnect();
	}

	@Override
	public String discoverProjectAreaUUID(String projectAreaName) throws IOException {
		HttpURLConnection conn = openGet(server + "/rootservices", "application/rdf+xml");
		String xml = readResponse(conn);
		conn.disconnect();

		// Simple scan for service provider entries containing the project area title
		// and an about/resource URL that ends with the UUID.
		// This is deliberately lenient to accommodate minor format variations.
		String lower = xml.toLowerCase(Locale.ROOT);
		String needle = ("<dcterms:title>" + projectAreaName + "</dcterms:title>").toLowerCase(Locale.ROOT);
		int idx = lower.indexOf(needle);
		if (idx < 0) {
			throw new RuntimeException("Project area not found in rootservices: " + projectAreaName);
		}
		// Walk backwards to find a nearby rdf:about or oslc:serviceProvider URI
		int aboutIdx = lower.lastIndexOf("rdf:about=\"", idx);
		if (aboutIdx < 0) {
			aboutIdx = lower.lastIndexOf("resource=\"", idx);
		}
		if (aboutIdx < 0) {
			throw new RuntimeException("Could not locate provider URL for project area: " + projectAreaName);
		}
		int start = lower.indexOf('"', aboutIdx) + 1;
		int end = lower.indexOf('"', start);
		String providerUrl = xml.substring(start, end);
		// UUID is the last path segment
		String uuid = providerUrl.substring(providerUrl.lastIndexOf('/') + 1);
		if (!uuid.startsWith("_")) {
			// Some deployments include /contexts/<uuid>
			int u2 = providerUrl.indexOf("/contexts/");
			if (u2 > 0)
				uuid = providerUrl.substring(u2 + "/contexts/".length());
		}
		return uuid;
	}

	@Override
	public List<WorkItem> queryAllWorkItems(String projectAreaUUID, String whereClause, int pageSize,
			List<String> customAttributeIds // e.g. ["com.ibm.team.workitem.attribute.my_attr"]
	) throws IOException {
		List<WorkItem> out = new ArrayList<>();

		// Prefixes to shorten nested selections and resolve titles of references
		String prefixes = String.join(",", "dcterms=<http://purl.org/dc/terms/>",
				"rtc_cm=<http://jazz.net/xmlns/prod/jazz/rtc/cm/1.0/>", "oslc=<http://open-services.net/ns/core#>",
				"foaf=<http://xmlns.com/foaf/0.1/>");

		// Build oslc.select including nested properties for referenced resources
		StringBuilder select = new StringBuilder();
		select.append("dcterms:identifier,dcterms:title,dcterms:created,dcterms:modified");
		select.append(",rtc_cm:state{dcterms:title}");
		select.append(",rtc_cm:type{dcterms:title}");
		select.append(",rtc_cm:ownedBy{foaf:name}");
		select.append(",rtc_cm:creator{foaf:name}");
		select.append(",rtc_cm:priority{dcterms:title}");
		select.append(",rtc_cm:severity{dcterms:title}");
		select.append(",rtc_cm:filedAgainst{dcterms:title}");
		select.append(",rtc_cm:plannedFor{dcterms:title}");
		select.append(",rtc_cm:tags");
		// Include any custom attributes explicitly
		if (customAttributeIds != null) {
			for (String id : customAttributeIds) {
				// Allow nested selection to grab titles if the custom attr is a reference type
				select.append(",rtc_cm:").append(id).append("{dcterms:title,foaf:name}");
			}
		}

		String base = server + "/oslc/contexts/" + projectAreaUUID + "/workitems" + "?oslc.prefix=" + url(prefixes)
				+ "&oslc.select=" + url(select.toString()) + "&oslc.pageSize=" + pageSize;
		if (whereClause != null && !whereClause.isBlank()) {
			base += "&oslc.where=" + url(whereClause);
		}

		String next = base;
		while (next != null) {
			HttpURLConnection conn = openGet(next, "application/json");
			String json = readResponse(conn);
			conn.disconnect();

			JsonNode root = mapper.readTree(json);
			JsonNode results = root.get("oslc:results");
			if (results != null && results.isArray()) {
				for (JsonNode node : results) {
					out.add(parseWorkItem(node, customAttributeIds));
				}
			}
			JsonNode nextPage = root.get("oslc:nextPage");
			next = (nextPage != null && !nextPage.isNull()) ? nextPage.asText(null) : null;
		}
		return out;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Map<String, List<URI>> getAllLinks(String workItemId) throws IOException {
		String url = server + "/resource/itemName/com.ibm.team.workitem.WorkItem/" + workItemId + "?oslc.select=*";

		HttpURLConnection conn = openGet(url, "application/json");
		String json = readResponse(conn);
		conn.disconnect();

		JsonNode root = mapper.readTree(json);
		Map<String, List<URI>> links = new HashMap<>();

		// Use fields() despite deprecation; safe in modern Jackson
		root.fields().forEachRemaining(entry -> {
			JsonNode value = entry.getValue();
			if (value.isArray() && value.size() > 0 && value.get(0).has("rdf:resource")) {
				List<URI> targets = new ArrayList<>();
				value.forEach(linkNode -> {
					try {
						String uriStr = linkNode.get("rdf:resource").asText();
						if (uriStr != null && !uriStr.isBlank()) {
							targets.add(URI.create(uriStr));
						}
					} catch (Exception ignored) {
					}
				});
				if (!targets.isEmpty()) {
					links.put(entry.getKey(), targets);
				}
			}
		});

		return links;
	}

	@Override
	public List<WorkItem> fetchByQueryId(String projectAreaUUID, String queryId) throws IOException {
		String queryUrl = server + "/oslc/queries/" + projectAreaUUID + "/urn:com.ibm.team.workitem.query:" + queryId
				+ "?oslc.pageSize=50";
		return queryFromUrl(queryUrl);
	}

	@Override
	public List<WorkItem> fetchOpenDefects(String projectAreaUUID) throws IOException {
		String where = "rtc_cm:type=\"defect\" and rtc_cm:state{rdf:resource!=\"Closed\"}";
		return queryWithWhere(projectAreaUUID, where);
	}

	@Override
	public List<WorkItem> fetchByOwner(String projectAreaUUID, String ownerUserId) throws IOException {
		String ownerUrl = server + "/resource/user/" + ownerUserId;
		String where = "rtc_cm:ownedBy=\"" + ownerUrl + "\"";
		return queryWithWhere(projectAreaUUID, where);
	}

	@Override
	public List<WorkItem> queryWithWhere(String projectAreaUUID, String whereClause) throws IOException {
		String queryUrl = server + "/oslc/contexts/" + projectAreaUUID + "/workitems?oslc.select="
				+ URLEncoder.encode(defaultSelect(), "UTF-8") + "&oslc.where=" + URLEncoder.encode(whereClause, "UTF-8")
				+ "&oslc.pageSize=50";
		return queryFromUrl(queryUrl);
	}

	/**
	 * Central place to execute a query starting from a URL and following pages.
	 */
	private List<WorkItem> queryFromUrl(String queryUrl) throws IOException {
		List<WorkItem> items = new ArrayList<>();
		while (queryUrl != null) {
			HttpURLConnection conn = openGet(queryUrl, "application/json");
			String json = readResponse(conn);
			conn.disconnect();

			JsonNode root = mapper.readTree(json);
			JsonNode results = root.get("oslc:results");
			if (results != null && results.isArray()) {
				for (JsonNode node : results) {
					WorkItem wi = mapper.treeToValue(node, WorkItem.class);
					items.add(wi);
				}
			}
			JsonNode nextPage = root.get("oslc:nextPage");
			queryUrl = (nextPage != null) ? nextPage.asText(null) : null;
		}
		return items;
	}

	/**
	 * Default select clause for queries — adjust this in one place for all helpers.
	 */
	private String defaultSelect() {
		return "dcterms:identifier,dcterms:title,dcterms:created,dcterms:modified,"
				+ "rtc_cm:state,rtc_cm:type,rtc_cm:ownedBy,rtc_cm:priority,rtc_cm:severity,"
				+ "rtc_cm:filedAgainst,rtc_cm:plannedFor,rtc_cm:tags";
	}

	private WorkItem parseWorkItem(JsonNode n, List<String> customIds) {
		Map<String, Object> custom = new LinkedHashMap<>();

		if (customIds != null) {
			for (String id : customIds) {
				String key = "rtc_cm:" + id;
				JsonNode val = n.get(key);
				if (val == null || val.isNull())
					continue;

				Object parsed;
				if (val.isObject() || val.has("rdf:resource") || val.has("dcterms:title") || val.has("foaf:name")) {
					parsed = readResource(n, key); // returns Optional<ResourceRef>
				} else if (val.isArray()) {
					parsed = StreamSupport.stream(val.spliterator(), false).map(JsonNode::asText).toList();
				} else {
					parsed = val.asText();
				}

				custom.put(id, parsed);
			}
		}

		return new WorkItem(readInt(n, "dcterms:identifier"), readStr(n, "dcterms:title"),
				readInstant(n, "dcterms:created"), readInstant(n, "dcterms:modified"), readResource(n, "rtc_cm:state"),
				readResource(n, "rtc_cm:type"), readResource(n, "rtc_cm:ownedBy", "foaf:name"),
				readResource(n, "rtc_cm:creator", "foaf:name"), readResource(n, "rtc_cm:priority"),
				readResource(n, "rtc_cm:severity"), readResource(n, "rtc_cm:filedAgainst"),
				readResource(n, "rtc_cm:plannedFor"), readTags(n, "rtc_cm:tags"), custom);
	}

	// ----------------------------- Helpers -----------------------------
	private HttpURLConnection openGet(String url, String accept) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Accept", accept);
		if (cookies != null)
			conn.setRequestProperty("Cookie", cookies);
		return conn;
	}

	private static String readResponse(HttpURLConnection conn) throws IOException {
		InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null)
				sb.append(line).append('\n');
			return sb.toString();
		}
	}

	private static String url(String s) throws UnsupportedEncodingException {
		return URLEncoder.encode(s, "UTF-8");
	}

	private static int readInt(JsonNode n, String key) {
		return n.has(key) ? n.get(key).asInt() : 0;
	}

	private static String readStr(JsonNode n, String key) {
		return n.has(key) ? n.get(key).asText(null) : null;
	}

	private static Instant readInstant(JsonNode n, String key) {
		String v = readStr(n, key);
		if (v == null || v.isBlank())
			return null;
		try {
			return Instant.parse(v);
		} catch (Exception e) {
			return null;
		}
	}

	private static ResourceRef readResource(JsonNode n, String key) {
		return readResource(n, key, "dcterms:title");
	}

	private static ResourceRef readResource(JsonNode n, String key, String titleField) {
		JsonNode obj = n.get(key);
		if (obj == null || obj.isNull())
			return null;
		if (obj.isTextual())
			return new ResourceRef(null, obj.asText());
		String uri = obj.has("rdf:resource") ? obj.get("rdf:resource").asText(null) : null;
		String title = obj.has(titleField) ? obj.get(titleField).asText(null)
				: (obj.has("dcterms:title") ? obj.get("dcterms:title").asText(null) : null);
		return new ResourceRef(uri, title);
	}

	private static List<String> readTags(JsonNode n, String key) {
		JsonNode t = n.get(key);
		if (t == null || t.isNull())
			return Collections.emptyList();
		if (t.isArray()) {
			List<String> out = new ArrayList<>();
			for (JsonNode x : t)
				out.add(x.asText());
			return out;
		}
		String s = t.asText("");
		if (s.isBlank())
			return Collections.emptyList();
		// EWM often stores tags as space-separated
		String[] parts = s.split("\\s+");
		return Arrays.asList(parts);
	}

}
