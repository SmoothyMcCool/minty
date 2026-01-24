package tom.tasks.transform.confluence;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.confluence.ConfluenceClient;
import tom.confluence.model.PageResponse;
import tom.tasks.TaskGroup;

@RunnableTask
public class ConfluenceQuery implements MintyTask {

	private TaskLogger logger;
	private ConfluenceQueryConfig config;
	private Packet input;
	private String error;
	private List<? extends OutputPort> outputs;
	private boolean failed;
	private ConfluenceClient confluenceClient;

	public ConfluenceQuery() {
		config = null;
		input = null;
		error = null;
		outputs = null;
		failed = false;
		confluenceClient = null;
	}

	public ConfluenceQuery(ConfluenceQueryConfig data) {
		this();
		config = data;
		confluenceClient = new ConfluenceClient(config.getBaseUrl(), config.getUsername(), config.getApiKey(),
				config.getUseBearerAuth(), config.getMaxPageCharacters());
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

	@Override
	public void run() {

		for (String pageId : config.getPages()) {
			PageResponse response = confluenceClient.getPage(pageId);

			Packet output = new Packet();
			output.addText(response.getBodyText());
			output.addDataList(input.getData());
			output.setId(input.getId());
			outputs.get(0).write(output);

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
				return TaskGroup.EXTERNAL.toString();
			}

		};
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}

}
