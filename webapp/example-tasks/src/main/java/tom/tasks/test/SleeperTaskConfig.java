package tom.tasks.test;

import java.util.HashMap;
import java.util.Map;

import tom.task.TaskConfig;
import tom.task.TaskConfigTypes;

public class SleeperTaskConfig implements TaskConfig {

	private int delay;

	public SleeperTaskConfig() {
		delay = 0;
	}

	public SleeperTaskConfig(Map<String, String> config) {
		delay = Integer.parseInt(config.get("delay"));
	}

	@Override
	public Map<String, TaskConfigTypes> getConfig() {
		Map<String, TaskConfigTypes> cfg = new HashMap<>();
		cfg.put("delay", TaskConfigTypes.Number);
		return cfg;
	}

	public int getDelay() {
		return delay;
	}

	public void setDelay(int delay) {
		this.delay = delay;
	}
}
