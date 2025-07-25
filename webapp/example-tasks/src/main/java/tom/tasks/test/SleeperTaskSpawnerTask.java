package tom.tasks.test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Spawn Sleeper Tasks", configClass = "tom.tasks.test.SleeperTaskSpawnerTaskConfig")
public class SleeperTaskSpawnerTask implements AiTask {

	private final Logger logger = LogManager.getLogger(SleeperTaskSpawnerTask.class);

	private SleeperTaskSpawnerTaskConfig config;
	private Instant startTime;

	public SleeperTaskSpawnerTask(SleeperTaskSpawnerTaskConfig config) {
		this.config = config;
		startTime = Instant.now();
	}

	@Override
	public String taskName() {
		return "TestTask-" + this.config.getDelay() + "-" + startTime.getEpochSecond();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> result = new HashMap<>();
		result.put("planName", "I slept for " + config.getDelay() + " seconds!");
		return result;
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		doTheThing();

		List<Map<String, String>> output = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			int sleepTime = new Random().nextInt(3) + 1;
			Map<String, String> cfg = Map.of("Delay", Integer.toString(sleepTime));
			output.add(cfg);
		}

		return output;
	}

	@Override
	public List<AiTask> runTask() {
		doTheThing();

		List<AiTask> childTasks = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			int sleepTime = new Random().nextInt(3) + 1;
			SleeperTaskConfig sleeperConfig = new SleeperTaskConfig();
			sleeperConfig.setDelay(sleepTime);
			childTasks.add(new SleeperTask(sleeperConfig));
		}

		return childTasks;
	}

	@Override
	public void setInput(Map<String, String> input) {
		// This task intentionally ignores all input, because you're allowed to do that.
	}

	private void doTheThing() {
		startTime = Instant.now();

		try {
			for (int i = 0; i < config.getDelay(); i++) {
				logger.info(taskName() + ": Sleeping... " + i);
				Thread.sleep(Duration.ofSeconds(1));
			}
		} catch (InterruptedException e) {
			logger.error("Sleep interrupted.");
		}
	}
}
