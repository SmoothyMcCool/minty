package tom.task;

import java.util.HashMap;
import java.util.Map;

public class NullTaskConfig implements TaskConfig {

	public NullTaskConfig() {
	}

	public NullTaskConfig(Map<String, String> config) {
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		return new HashMap<>();
	}

}
