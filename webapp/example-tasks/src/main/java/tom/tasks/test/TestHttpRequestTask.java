package tom.tasks.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.api.services.TaskServices;
import tom.task.MintyTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicTask;

// This class defines no constructor that takes a configuration object because
// it defines no set-time configuration. It still takes in input from previous tasks though,
// so it can be controlled as a step in a workflow.
@PublicTask(name = "TestHttpRequest")
public class TestHttpRequestTask implements MintyTask, ServiceConsumer {

	private TaskServices taskServices;
	private UUID userId;
	private TestUser user = null;

	@Override
	public String taskName() {
		return "TestHttpRequest-" + UUID.randomUUID();
	}

	@Override
	public Map<String, Object> getResult() {
		if (user == null) {
			return new HashMap<>();
		}

		Map<String, Object> result = new HashMap<>();
		result.put("name", user.getName());
		result.put("password", user.getPassword());
		result.put("id", user.getId());

		return result;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		user = taskServices.getHttpService().getBasicAuth(userId, "http://localhost:8080/Minty/api/login", null,
				new TestUser());
		return List.of();
	}

	@Override
	public void setInput(Map<String, Object> input) {
		if (input.containsKey("UserName")) {
			Object userId = input.get("UserName");
			if (userId instanceof Integer) {
				userId = input.get("UserName");
			}
		}
	}

	@Override
	public String expects() {
		return "Reads key Username as string if it exists in the input.";
	}

	@Override
	public String produces() {
		return "{ \"User Name\": <String> }";
	}

}
