package tom.tasks.emit.confluence;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;

@RunnableTask
public class ConfluenceQuery implements MintyTask {

	private TaskLogger logger;
	private ConfluenceQueryConfig config;
	private Packet input;
	private String error;
	private List<? extends OutputPort> outputs;
	private boolean failed;

	public ConfluenceQuery() {
		config = null;
		input = null;
		error = null;
		outputs = null;
		failed = false;
	}

	public ConfluenceQuery(ConfluenceQueryConfig data) {
		this();
		config = data;
	}

	@Override
	public void inputTerminated(int i) {
		// Don't care.
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public Packet getResult() {
		return null;
	}

	@Override
	public String getError() {
		return error;
	}

	private String clean(String html) {
		if (html == null) {
			return "";
		}

		// Get rid of some confluence macros.
		return html.replaceAll("(?s)<ac:[^>]+>.*?</ac:[^>]+>", "").replaceAll("(?s)<ri:[^>]+>.*?</ri:[^>]+>", "");
	}

	@Override
	public void run() {
		String auth = config.getUsername() + ":" + config.getApiKey();
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		String authString = config.getUseBearerAuth() ? "Bearer " + config.getApiKey() : "Basic " + encodedAuth;

		ObjectMapper mapper = new ObjectMapper();

		try (CloseableHttpClient client = HttpClients.createDefault()) {

			for (String pageId : config.getPages()) {
				final String baseUrl = config.getBaseUrl().replaceAll("/+$", "");
				final String completeUrl = baseUrl + "/rest/api/content/" + pageId + "?expand=body.storage";
				HttpGet request = new HttpGet(completeUrl);
				request.addHeader("Authorization", authString);
				request.addHeader("Accept", "application/json");

				logger.debug("ConfluenceQuery: Fetching page: " + completeUrl);

				HttpClientResponseHandler<String> responseHandler = response -> {
					int statusCode = response.getCode();

					if (statusCode != HttpStatus.SC_OK) {
						throw new RuntimeException("Confluence didn't return 200 OK for pageId " + pageId);
					}

					return EntityUtils.toString(response.getEntity());
				};

				String responseBody = client.execute(request, responseHandler);

				JsonNode root = mapper.readTree(responseBody);
				String pageText = root.path("body").path("storage").path("value").asText();
				pageText = clean(pageText);

				Packet output = new Packet();
				output.addText(pageText);
				output.addDataList(input.getData());
				output.setId(input.getId());
				outputs.get(0).write(output);

			}
		} catch (Exception e) {
			error = "Caught exception while fetching page: " + e.toString();
			throw new RuntimeException("ConfluenceQuery: Caught exception while fetching page.", e);
		}

	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"Workflow misconfiguration detect. ConfluenceQuery should only ever have exactly one input!");
		}
		if (dataPacket.getData().size() != 1) {
			failed = true;
			throw new RuntimeException("Packet must contain exactly one data element.");
		}
		try {
			List<Map<String, Object>> dataList = dataPacket.getData();
			for (Map<String, Object> data : dataList) {
				config.updateFrom(data);
			}
			input = dataPacket;
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Received malformed data as input.");
		}
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return true;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Retreive the contents of a page on Confluence. Uses Confluence Page IDs.";
			}

			@Override
			public String expects() {
				return "If the \"Data\" contains a \"Pages\" key consisting of a list of pageIds, those Page IDs "
						+ "will be used instead of those provided in the config.\n\nData must contain exactly one element.";
			}

			@Override
			public String produces() {
				return "For each URL processed, emits a Packet with the \"Text\" set to the HTML body of the page.";
			}

			@Override
			public int numOutputs() {
				return 1;
			}

			@Override
			public int numInputs() {
				return 1;
			}

			@Override
			public TaskConfigSpec taskConfiguration() {
				return new ConfluenceQueryConfig();
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				try {
					return new ConfluenceQueryConfig(configuration);
				} catch (JsonProcessingException e) {
					throw new RuntimeException("Failed to read configuration.", e);
				}
			}

			@Override
			public String taskName() {
				return "Get Confluence Pages";
			}

			@Override
			public String group() {
				return "Emit";
			}

		};
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
