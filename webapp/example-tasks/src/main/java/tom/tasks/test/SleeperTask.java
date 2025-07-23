package tom.tasks.test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;
import tom.task.annotations.PublicTask;

@PublicTask(name = "Sleep", configClass = "tom.tasks.test.SleeperTaskConfig")
public class SleeperTask implements AiTask {

	private final Logger logger = LogManager.getLogger(SleeperTask.class);

	private final SleeperTaskConfig configuration;
	private final Instant startTime = Instant.now();

	public SleeperTask() {
		configuration = new SleeperTaskConfig();
	}

	public SleeperTask(SleeperTaskConfig configuration) {
		this.configuration = configuration;
	}

	@Override
	public String taskName() {
		return "SleeperTask-" + configuration.getDelay() + "-" + startTime.getEpochSecond();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> results = new HashMap<>();

		results.put("Result", "I slept for " + configuration.getDelay() + " seconds.");

		return results;
	}

	@Override
	public List<Map<String, String>> runWorkflow() {
		doTheThing();
		return List.of();
	}

	@Override
	public List<AiTask> runTask() {
		doTheThing();
		return List.of();
	}

	@Override
	public void setInput(Map<String, String> input) {
		if (input.containsKey("delay")) {
			configuration.setDelay((Integer.parseInt(input.get("delay"))));
		}
	}

	private void doTheThing() {
		try {
			for (int i = 0; i < configuration.getDelay(); i++) {
				logger.info(taskName() + ": Sleeping... " + i);
				Thread.sleep(Duration.ofSeconds(1));
			}
		} catch (InterruptedException e) {
			logger.error("Sleep interrupted.");
		}
	}
}
