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
	public List<Map<String, String>> runTask() {
		return List.of();
	}

	@Override
	public String expects() {
		return "This task expects no input. It doesn't matter what you provide.";
	}

	@Override
	public String produces() {
		return "This task produces no output. Using it will cause your workflow to stop running.";
	}

}
