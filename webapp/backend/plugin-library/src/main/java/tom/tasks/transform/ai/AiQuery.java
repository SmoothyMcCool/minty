package tom.tasks.transform.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskLogger;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;

@RunnableTask
public class AiQuery implements MintyTask, ServiceConsumer {

	private final String ConversationId = "Conversation ID";

	private TaskLogger logger;
	private PluginServices pluginServices;
	private AiQueryConfig config;
	private UserId userId;
	private Packet result;
	private Packet input;
	private String error;
	private List<? extends OutputPort> outputs;
	private String conversationId;
	private boolean failed;

	public AiQuery() {
		pluginServices = null;
		config = new AiQueryConfig();
		userId = new UserId("");
		result = new Packet();
		input = null;
		error = null;
		outputs = null;
		conversationId = null;
		failed = false;
	}

	public AiQuery(AiQueryConfig data) {
		this();
		config = data;
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public Packet getResult() {
		return result;
	}

	@Override
	public String getError() {
		return error;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}

	private Packet parseResponse(String response) {

		if (StringUtils.isBlank(response)) {
			return result;
		}

		try {
			ObjectMapper mapper = new ObjectMapper();

			List<Map<String, Object>> resultAsMap = null;
			try {
				resultAsMap = mapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {
				});
			} catch (Exception e) {
				// If we fail, remove markdown fences if present and try again, since some LLMs
				// give us markdown fences even if we dont want them.
				String noMarkdownFences = response.replaceAll("(?s)^\\s*```[a-zA-Z0-9_-]*\\s*", "")
						.replaceAll("(?s)\\s*```\\s*$", "").trim();
				resultAsMap = mapper.readValue(noMarkdownFences, new TypeReference<List<Map<String, Object>>>() {
				});
			}
			if (resultAsMap != null) {
				result.setData(resultAsMap);
			} else {
				result.addText(response);
			}
			return result;

		} catch (Exception e) {
			// If we catch an exception it probably wasn't valid JSON :)
			result.addText(response);
			return result;
		}
	}

	@Override
	public void run() {
		AssistantQuery query = new AssistantQuery();
		query.setAssistantSpec(config.getAssistant());

		List<String> queries = input != null ? input.getText() : List.of(config.getQuery());

		for (String text : queries) {
			if (StringUtils.isNotBlank(text)) {
				query.setQuery(config.getQuery() + " " + text);
			} else {
				query.setQuery(config.getQuery());
			}

			if (conversationId != null && query.getAssistantSpec().useId()
					&& pluginServices.getAssistantManagementService()
							.isAssistantConversational(query.getAssistantSpec().getAssistantId())) {
				query.setConversationId(new ConversationId(conversationId));
			} else {
				query.setConversationId(new ConversationId(UUID.randomUUID()));
			}

			try {
				ConversationId requestId = null;
				while (true) {
					try {
						requestId = pluginServices.getAssistantQueryService().ask(userId, query);
						break;

					} catch (QueueFullException | ConversationInUseException e) {
						logger.debug(
								"AiQuery: LLM queue is full or conversation in use. Sleeping for 5 seconds before trying again.",
								e);
						Thread.sleep(Duration.ofSeconds(5));
					}
				}

				String response = null;
				while (true) {
					StringResult llmResult = (StringResult) pluginServices.getAssistantQueryService()
							.getResultFor(requestId);
					if (llmResult != null && llmResult.isComplete()) {
						response = llmResult instanceof StringResult ? ((StringResult) llmResult).getValue() : null;
						break;
					}
					logger.debug("AiQuery: LLM response not ready. Sleeping for 5 seconds before trying again.");
					Thread.sleep(Duration.ofSeconds(5));
				}

				result = parseResponse(response);

				result.setId(input != null ? input.getId() : "");
				if (conversationId != null) {
					result.getData().forEach(item -> item.put(ConversationId, conversationId));
				}

			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("AiQuery Task got interrupted while waiting for its turn with the LLM.");
			}
		}

		outputs.get(0).write(result);
	}

	@Override
	public boolean giveInput(int inputNum, Packet dataPacket) {
		if (inputNum != 0) {
			failed = true;
			throw new RuntimeException(
					"AiQuery: Workflow misconfiguration detect. AiQuery should only ever have exactly one input!");
		}

		input = dataPacket;

		if (dataPacket.getData().size() > 0) {
			Map<String, Object> data = dataPacket.getData().getFirst();
			if (data != null && data.containsKey(ConversationId)) {
				conversationId = (String) data.get(ConversationId);
			}
		}
		return true;
	}

	@Override
	public void setOutputConnectors(List<? extends OutputPort> outputs) {
		this.outputs = outputs;
	}

	@Override
	public boolean readyToRun() {
		return input != null;
	}

	@Override
	public TaskSpec getSpecification() {
		return new TaskSpec() {

			@Override
			public String description() {
				return "Perform a query to the LLM, either based on the input received, or the configuration of the task.";
			}

			@Override
			public String expects() {
				return "This task appends the contents of \"text\" to the provided prompt. It will use the conversation defined by "
						+ "the input \"data.Conversation ID\", provided by the workflow runner, that is used to continue an AI conversation. "
						+ "NOTE!!! For this to work, you MUST choose an Assistant that has memory enabled!\n\nThe input to this task must "
						+ "not contain a list of data items (0 or 1 items in the data list).";
			}

			@Override
			public String produces() {
				return "This task produces the response from the AI as an output to the next task, "
						+ "in the form defined by the AI assistant. The response will be emitted in the \"Text\" "
						+ "field, with the value set to the response from the LLM. If the response can be mapped to "
						+ "Map<String, Object> (e.g. JSON), that will be returned in \"Data\"";
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
				return new AiQueryConfig(Map.of(AiQueryConfig.Assistant,
						AssistantManagementService.DefaultAssistantId.getValue().toString()));
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				return new AiQueryConfig(configuration);
			}

			@Override
			public String taskName() {
				return "Query LLM";
			}

			@Override
			public String group() {
				return TaskGroup.TRANSFORM.toString();
			}

		};
	}

	@Override
	public void inputTerminated(int i) {
		// Nothing to do, don't care.
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public void setLogger(TaskLogger workflowLogger) {
		this.logger = workflowLogger;
	}
}
