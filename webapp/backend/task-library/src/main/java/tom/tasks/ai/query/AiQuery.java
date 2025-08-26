package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import tom.api.services.TaskServices;
import tom.model.AssistantQuery;
import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Query the Robot", configClass = "tom.tasks.ai.query.AiQueryConfig")
public class AiQuery implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID uuid = UUID.randomUUID();
	private AiQueryConfig config = new AiQueryConfig();
	private UUID userId;
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
	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		try {
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

			String response = taskServices.getAssistantQueryService().ask(userId, query);

			Map<String, Object> responseAsMap = parseResponse(response);

			if (!responseAsMap.isEmpty()) {
				result.put("Data", responseAsMap);
			} else {
				result.put("Data", response);
			}

			if (input.containsKey("Conversation ID")) {
				responseAsMap.put("Conversation ID", input.get("Conversation ID"));
			}

			return List.of(responseAsMap);
		} catch (Exception e) {
			error = "Failed to Query Assistant: " + e.toString();
		}
		return List.of();
	}

	private Map<String, Object> parseResponse(String response) {

		if (response == null || response.isBlank()) {
			return new HashMap<>();
		}

		try {
			ObjectMapper mapper = new ObjectMapper();

			Map<String, Object> map = mapper.readValue(response, new TypeReference<Map<String, Object>>() {
			});
			return map != null ? map : new HashMap<>();
		} catch (Exception e) {
			return new HashMap<>();
		}
	}

	@Override
	public void setInput(Map<String, Object> input) {
		config.updateFrom(input);
		this.input = input;
	}

	@Override
	public String expects() {
		return "This task appends the contents of \"data\" to the provided query.\n\nIt will not make use of any conversations.\n";
	}

	@Override
	public String produces() {
		return "This task produces the response from the AI as an output to the next task, "
				+ "in the form defined by the AI assistant.  If the task fails to create an output as a Map from the AI "
				+ "response, it will return a Map containing the key \"Data\", which holds the output from the AI as a string.";
	}
}
