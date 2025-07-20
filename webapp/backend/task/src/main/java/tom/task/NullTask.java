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
	public String getResultTemplateFilename() {
		return "";
	}

	@Override
	public List<AiTask> doWork() {
		return List.of();
	}

}
