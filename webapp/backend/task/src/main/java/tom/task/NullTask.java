package tom.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NullTask implements AiTask {

	@Override
	public String taskName() {
		return "Null Task";
	}

	@Override
	public Map<String, Object> getResult() {
		return new HashMap<>();
	}

	@Override
	public void setInput(Map<String, String> input) {
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		return List.of();
	}

	@Override
	public List<AiTask> runTask() {
		return List.of();
	}

}
