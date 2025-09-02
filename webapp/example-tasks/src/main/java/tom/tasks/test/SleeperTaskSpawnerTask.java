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

import tom.task.MintyTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Spawn Sleeper Tasks", configClass = "tom.tasks.test.SleeperTaskSpawnerTaskConfig")
public class SleeperTaskSpawnerTask implements MintyTask {

	private final Logger logger = LogManager.getLogger(SleeperTaskSpawnerTask.class);

	private final SleeperTaskSpawnerTaskConfig config;
	private Instant startTime;

	public SleeperTaskSpawnerTask() {
		this.config = new SleeperTaskSpawnerTaskConfig();
		startTime = Instant.now();
	}

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
		result.put("Spawn Sleeper Tasks", "I slept for " + config.getDelay() + " seconds!");
		return result;
	}

	@Override
	public String getError() {
		return null;
	}

	@Override
	public List<Map<String, Object>> runTask() {
		doTheThing();

		List<Map<String, Object>> output = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			int sleepTime = new Random().nextInt(3) + 1;
			Map<String, Object> cfg = Map.of("Data", Integer.valueOf(sleepTime));
			output.add(cfg);
		}

		return output;
	}

	@Override
	public void setInput(Map<String, Object> input) {
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

	@Override
	public String expects() {
		return "Ignores all input.";
	}

	@Override
	public String produces() {
		return "Produces one or more outputs of the form { \"Data\": <Integer> }";
	}
}
