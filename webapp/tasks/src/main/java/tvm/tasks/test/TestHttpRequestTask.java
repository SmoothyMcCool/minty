package tvm.tasks.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import tom.task.AiTask;
import tom.task.ServiceConsumer;
import tom.task.annotations.PublicWorkflow;
import tom.task.services.TaskServices;

@PublicWorkflow(name = "TestHttpRequest")
public class TestHttpRequestTask implements AiTask, ServiceConsumer {

	private TaskServices taskServices;
	private int userId;
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
		result.put("corpAccount", user.getCorpAccount());
		result.put("corpPassword", user.getCorpPassword());
		result.put("name", user.getName());
		result.put("password", user.getPassword());
		result.put("id", user.getId());

		return result;
	}

	@Override
	public String getResultTemplateFilename() {
		return "test-user.pug";
	}

	@Override
	public List<AiTask> doWork() {
		user = taskServices.getHttpService().getBasicAuth(userId, "http://localhost:8080/MrAi/api/login", null, new TestUser());
		return null;
	}

	@Override
	public void setTaskServices(TaskServices taskServices) {
		this.taskServices = taskServices;
	}

	@Override
	public void setUserId(int userId) {
		this.userId = userId;
	}


}
