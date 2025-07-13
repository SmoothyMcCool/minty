package tom.task;

import java.util.HashMap;
import java.util.Map;

public class NullTaskConfig implements AiTaskConfig {

	public NullTaskConfig() {
	}

	public NullTaskConfig(Map<String, String> config) {
	}

	@Override
	public Map<String, AiTaskConfigTypes> getConfig() {
		return new HashMap<>();
	}

}
