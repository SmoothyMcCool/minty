package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.model.AssistantQuery;
import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;
import tom.task.services.TaskServices;

@PublicTask(name = "Converse with Robot", configClass = "tom.tasks.ai.query.AiQueryConfig")
public class ConversationalAiQuery implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID uuid = UUID.randomUUID();
	private AiQueryConfig config = new AiQueryConfig();
	private int userId = 0;
	private Map<String, Object> result = new HashMap<>();
	private Map<String, String> input = Map.of();

	public ConversationalAiQuery() {

	}

	public ConversationalAiQuery(AiQueryConfig data) {
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
	public void setUserId(int userId) {
		this.userId = userId;
	}

	@Override
	public List<Map<String, String>> runTask() {

		Map<String, String> response = new HashMap<>();
		response.put("Data", doTheThing());

		result = parseResponse(response.get("Data"));

		return List.of(response);
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
	public void setInput(Map<String, String> input) {
		config.updateFrom(input);
		this.input = input;
	}

	private String doTheThing() {
		AssistantQuery query = new AssistantQuery();
		query.setAssistantId(config.getAssistant());

		if (input.containsKey("Data")) {
			query.setQuery(config.getQuery() + " " + input.get("Data"));
		} else {
			query.setQuery(config.getQuery());
		}

		if (input.containsKey("Conversation ID")) {
			query.setConversationId(input.get("Conversation ID"));
		}

		return taskServices.getAssistantQueryService().ask(userId, query);
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
