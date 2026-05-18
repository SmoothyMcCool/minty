package tom.tasks.transform.ai;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.tools.picocli.CommandLine.TypeConversionException;

import tom.api.ConversationId;
import tom.api.MintyObjectMapper;
import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.api.task.MintyTask;
import tom.api.task.OutputPort;
import tom.api.task.Packet;
import tom.api.task.TaskConfigSpec;
import tom.api.task.TaskSpec;
import tom.api.task.annotation.RunnableTask;
import tom.tasks.TaskGroup;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@RunnableTask
public class AiQuery extends MintyTask implements ServiceConsumer {

	private final String ConversationId = "Conversation ID";

	private PluginServices pluginServices;
	private AiQueryConfig config;
	private UserId userId;
	private Packet result;
	private Packet input;
	private String error;
	private List<? extends OutputPort> outputs;
	private ConversationId conversationId;
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
			ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;

			List<Map<String, Object>> resultAsMap = null;
			try {
				resultAsMap = mapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {
				});
			} catch (Exception e) {
				// If we fail, remove markdown fences if present and try again, since some LLMs
				// give us markdown fences even if we dont want them.
				String noMarkdownFences = response.replaceAll("(?s)^\\s*```[a-zA-Z0-9_-]*\\s*", "")
						.replaceAll("(?s)\\s*```\\s*$", "").strip();
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
				query.setConversationId(conversationId);
			} else {
				query.setConversationId(new ConversationId(UUID.randomUUID()));
			}

			String response = null;
			while (true) {
				try {
					CompletableFuture<String> future = pluginServices.getAssistantQueryService().ask(userId, query);
					response = future.get();
					break;

				} catch (QueueFullException | ConversationInUseException e) {
					if (!sleepForRetry()) {
						throw new CancellationException("AiQuery interrupted while waiting to retry.");
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					pluginServices.getAssistantQueryService().cancelRequest(userId, query.getConversationId());
					throw new CancellationException("AiQuery task interrupted while waiting for LLM.");

				} catch (CancellationException e) {
					throw e; // propagate up

				} catch (ExecutionException e) {
					if (e.getCause() instanceof QueueFullException
							|| e.getCause() instanceof ConversationInUseException) {
						if (!sleepForRetry()) {
							throw new CancellationException("AiQuery interrupted while waiting to retry.");
						}
					} else {
						throw new RuntimeException("LLM call failed.", e.getCause());
					}
				}
			}

			result = parseResponse(response);

			result.setId(input != null ? input.getId() : "");
			if (conversationId != null) {
				result.getData().forEach(item -> item.put(ConversationId, conversationId));
			}
		}

		outputs.get(0).write(result);
	}

	private boolean sleepForRetry() {
		warn("LLM queue full or conversation in use, retrying in 5 seconds.");
		try {
			Thread.sleep(Duration.ofSeconds(5));
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			warn("Thread was interrupted while waiting to retry.");
			return false;
		}
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
				conversationId = new ConversationId((String) data.get(ConversationId));
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
				return "Send a prompt to an AI assistant and emit the response as a packet. "
						+ "The system prompt is set in the Assistant configuration. "
						+ "The Query field is the first user message - leave it empty when text is fed from an upstream step.";
			}

			@Override
			public String expects() {
				return "Accepts: a packet whose text items are each appended to the Query and sent to the model - "
						+ "one LLM call is made per text item. "
						+ "The input data array must contain 0 or 1 objects; multiple records are not supported. "
						+ "Both text and data must be arrays even when empty. "
						+ "To continue a conversation across calls, choose an assistant with memory enabled; "
						+ "the task carries the Conversation ID automatically through the packet.";
			}

			@Override
			public String produces() {
				return "Emits: a packet with the LLM response in data if the response is a valid JSON array of objects, "
						+ "or in text otherwise. " + "The output packet ID is copied from the input packet.";
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
				try {
					return new AiQueryConfig(Map.of(AssistantListEnumSpecCreator.EnumName,
							AssistantManagementService.DefaultAssistantId));
				} catch (TypeConversionException e) {
					return null;
				}
			}

			@Override
			public TaskConfigSpec taskConfiguration(Map<String, Object> configuration) {
				try {
					return new AiQueryConfig(configuration);
				} catch (TypeConversionException e) {
					return null;
				}
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

}
