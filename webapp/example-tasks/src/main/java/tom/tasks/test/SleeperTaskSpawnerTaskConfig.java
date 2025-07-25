package tom.tasks.test;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class SleeperTaskSpawnerTaskConfig implements TaskConfig {

	private int delay;

	public SleeperTaskSpawnerTaskConfig() {
		delay = 0;
	}

	public SleeperTaskSpawnerTaskConfig(Map<String, String> config) {
		delay = Integer.parseInt(config.get("Delay"));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("Delay", TaskConfigTypes.Number);
		return cfg;
	}

	public int getDelay() {
		return delay;
	}
}
