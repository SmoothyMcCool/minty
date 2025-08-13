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

@PublicTask(name = "Query the Robot", configClass = "tom.tasks.ai.query.AiQueryConfig")
public class AiQuery implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID uuid = UUID.randomUUID();
	private AiQueryConfig config = new AiQueryConfig();
	private int userId = 0;
	private Map<String, Object> result = new HashMap<>();
	private Map<String, String> input = Map.of();

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
	public void setUserId(int userId) {
		this.userId = userId;
	}

	@Override
	public List<Map<String, String>> runTask() {
		String response = doTheThing();
		Map<String, String> responseAsMap = parseResponse(response);

		if (input.containsKey("Conversation ID")) {
			result.put("Conversation ID", input.get("Conversation ID"));
		}

		if (!responseAsMap.isEmpty()) {
			result.put("Data", responseAsMap);
		} else {
			result.put("Data", response);
		}

		return List.of(responseAsMap);
	}

	private Map<String, String> parseResponse(String response) {

		if (response == null || response.isBlank()) {
			return new HashMap<>();
		}

		try {
			ObjectMapper mapper = new ObjectMapper();

			Map<String, String> map = mapper.readValue(response, new TypeReference<Map<String, String>>() {
			});
			return map != null ? map : new HashMap<>();
		} catch (Exception e) {
			return new HashMap<>();
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
			query.setConversationId(null);
		}

		return taskServices.getAssistantQueryService().ask(userId, query);
	}

	@Override
	public String expects() {
		return "This task appends the contents of \"data\" to the provided query. It will not make use of any conversations.";
	}

	@Override
	public String produces() {
		return "This task produces the response from the AI as an output to the next task, "
				+ "in the form defined by the AI assistant.  If the task fails to create an output as a Map from the AI "
				+ "response, it will return a Map containing the key \"Data\", which holds the output from the AI as a string.";
	}
}
