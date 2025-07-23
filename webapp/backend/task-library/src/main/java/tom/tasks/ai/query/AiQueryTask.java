package tom.tasks.ai.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;
import tom.task.model.AssistantQuery;
import tom.task.services.TaskServices;

@PublicTask(name = "Ask Assistant", configClass = "tom.tasks.ai.query.AiQueryTaskConfig")
public class AiQueryTask implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID uuid;
	private String result;
	private AiQueryTaskConfig config;
	private int userId;

	public AiQueryTask(AiQueryTaskConfig data) {
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
		Map<String, Object> result = new HashMap<>();
		result.put("result", this.result);
		return result;
	}

	@Override
	public void setUserId(int userId) {
		this.userId = userId;
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		return List.of(Map.of("AiResponse", doTheThing()));
	}

	@Override
	public List<AiTask> runTask() {
		doTheThing();
		return null;
	}

	@Override
	public void setInput(Map<String, String> input) {
		config.updateFrom(input);
	}

	private String doTheThing() {
		AssistantQuery query = new AssistantQuery();
		query.setAssistantId(config.getAssistant());
		query.setQuery(config.getQuery());
		query.setConversationId(config.getConversationId());

		return taskServices.getAssistantService().ask(userId, query);
	}
}
