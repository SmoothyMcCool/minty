package tom.tasks.ai.query;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.services.TaskServices;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.model.AssistantQuery;
import tom.task.MintyTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Converse with Robot", configClass = "tom.tasks.ai.query.AiQueryConfig")
public class AiQuery implements MintyTask, ServiceConsumer {

	private final Logger logger = LogManager.getLogger(AiQuery.class);

	private TaskServices taskServices;
	private UUID uuid = UUID.randomUUID();
	private AiQueryConfig config = new AiQueryConfig();
	private UserId userId;
	private Map<String, Object> result = new HashMap<>();
	private Map<String, Object> input = Map.of();
	private String error = null;

	public AiQuery() {

	}

	public AiQuery(AiQueryConfig data) {
		config = data;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
		uuid = UUID.randomUUID();
	}

	@Override
	public String taskName() {
		return "AskAssistant-" + uuid;
	}

	@Override
	public Map<String, Object> getResult() {
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

	@Override
	public List<Map<String, Object>> runTask() {

		AssistantQuery query = new AssistantQuery();
		query.setAssistantId(config.getAssistant());

		if (input.containsKey("Data")) {
			query.setQuery(config.getQuery() + " " + input.get("Data"));
		} else {
			query.setQuery(config.getQuery());
		}

		if (input.containsKey("Conversation ID")) {
			query.setConversationId(new ConversationId(input.get("Conversation ID").toString()));
		}

		try {
			ConversationId requestId = null;
			while (true) {
				try {
					requestId = taskServices.getAssistantQueryService().ask(userId, query);
					break;

				} catch (QueueFullException | ConversationInUseException e) {
					logger.warn(
							"LLM queue is full or conversation in use. Sleeping for 5 seconds before trying again.");
					Thread.sleep(Duration.ofSeconds(5));
				}
			}

			String response = null;
			while (true) {
				StringResult llmResult = (StringResult) taskServices.getAssistantQueryService().getResultFor(requestId);
				if (llmResult != null && llmResult.isComplete()) {
					response = llmResult instanceof StringResult ? ((StringResult) llmResult).getValue() : null;
					break;
				}
				logger.warn("LLM response not ready. Sleeping for 5 seconds before trying again.");
				Thread.sleep(Duration.ofSeconds(5));
			}

			result = parseResponse(response);
		} catch (InterruptedException e) {
			throw new RuntimeException(
					"Task " + taskName() + " got interrupted while waiting for its turn with the LLM.");
		}

		return List.of(result);
	}

	private Map<String, Object> parseResponse(String response) {

		Map<String, Object> result = new HashMap<>();
		result.put("Data", response);

		if (response == null || response.isBlank()) {
			return result;
		}

		try {
			ObjectMapper mapper = new ObjectMapper();

			Map<String, String> resultAsMap = mapper.readValue(response, new TypeReference<Map<String, String>>() {
			});
			if (resultAsMap != null) {
				result.put("Data", resultAsMap);
			}
			return result;

		} catch (Exception e) {
			return result;
		}
	}

	@Override
	public void setInput(Map<String, Object> input) {
		config.updateFrom(input);
		this.input = input;
	}

	@Override
	public String expects() {
		return "This task appends the contents of \"data\" to the provided prompt. It will use the conversation defined by "
				+ "the input \"Conversation ID\", provided by the workflow runner, that is used to continue an AI conversation. "
				+ "NOTE!!! For this to work, you MUST choose an Assistant that has memory enabled!";
	}

	@Override
	public String produces() {
		return "This task produces the response from the AI as an output to the next task, "
				+ "in the form defined by the AI assistant. The response will be emitted as an Oject with a Single \"Data\" "
				+ "key, with the value set to the response from the AI";
	}
}
