package tom.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NullTask implements MintyTask {

	@Override
	public String taskName() {
		return "Null Task";
	}

	@Override
	public Map<String, Object> getResult() {
		return new HashMap<>();
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public void setInput(Map<String, Object> input) {
	}

	@Override
	public List<Map<String, Object>> runTask() {
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
