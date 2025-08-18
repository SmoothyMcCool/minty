package tom.tasks.extractor.confluence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Get Confluence Pages", configClass = "tom.tasks.extractor.confluence.ConfluenceQueryConfig")
public class ConfluenceQuery implements AiTask {

	private UUID uuid = UUID.randomUUID();
	private ConfluenceQueryConfig config = new ConfluenceQueryConfig();

	public ConfluenceQuery() {
	}

	public ConfluenceQuery(ConfluenceQueryConfig data) {
		config = data;
	}

	@Override
	public String taskName() {
		return "ConfluenceQuery-" + uuid;
	}

	@Override
	public Map<String, Object> getResult() {
		return Map.of();
	}

	@Override
	public List<Map<String, Object>> runTask() {
		String auth;
		if (!config.getApiKey().isBlank()) {
			auth = config.getUsername() + ":" + config.getPassword();
		} else {
			auth = config.getUsername() + ":" + config.getPassword();
		}

		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

		HttpClient client = HttpClients.createDefault();
		ObjectMapper mapper = new ObjectMapper();

		List<Map<String, Object>> output = new ArrayList<>();

		for (String pageId : config.getPages()) {
			final String baseUrl = config.getBaseUrl().replaceAll("/+$", "");
			final String completeUrl = baseUrl + "/rest/api/content/" + pageId + "?expand=body.storage";
			HttpGet request = new HttpGet(completeUrl);
			request.addHeader("Authorization", "Basic " + encodedAuth);
			request.addHeader("Accept", "application/json");

			try (ClassicHttpResponse response = client.executeOpen(null, request, null)) {
				int statusCode = response.getCode();

				if (statusCode != HttpStatus.SC_OK) {
					throw new RuntimeException("Confluence didn't return 200 OK for pageId " + pageId);
				}

				String responseBody = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";

				JsonNode root = mapper.readTree(responseBody);
				String pageText = root.path("body").path("storage").path("value").asText();
				pageText = clean(pageText);

				output.add(Map.of("Data", pageText));

			} catch (IOException | ParseException e) {
				throw new RuntimeException("Failed to make request for page " + pageId, e);
			}
		}

		return output;
	}

	private String clean(String html) {
		if (html == null) {
			return "";
		}

		// Get rid of some confluence macros.
		return html.replaceAll("(?s)<ac:[^>]+>.*?</ac:[^>]+>", "").replaceAll("(?s)<ri:[^>]+>.*?</ri:[^>]+>", "");
	}

	@Override
	public void setInput(Map<String, Object> input) {
		if (input.containsKey("Data")) {
			try {
				config.updateFrom(input);
			} catch (JsonProcessingException e) {
				throw new RuntimeException("Received malformed data as input.");
			}
		}
	}

	@Override
	public String expects() {
		return "If the \"Data\" contains a URL or list of URLs, those URLs will be used instead of those provided in the config.";
	}

	@Override
	public String produces() {
		return "For each URL processed, emits a record with a single \"Data\" element containing the HTML body of the page.";
	}
}
