package tvm.tasks.test;

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
import tom.task.annotations.PublicWorkflow;

@PublicWorkflow(name = "TestWorkflow", configClass = "tvm.tasks.test.TeskWorkflowTaskConfig")
public class TestWorkflowTask implements AiTask {

	private final Logger logger = LogManager.getLogger(TestWorkflowTask.class);

	private TeskWorkflowTaskConfig config;
	private Instant startTime;

	public TestWorkflowTask(TeskWorkflowTaskConfig config) {
		this.config = config;
		startTime = Instant.now();
	}

	@Override
	public String taskName() {
		return "TestWorkflow-" + this.config.getDelay() + "-" + startTime.getEpochSecond();
	}

	@Override
	public List<AiTask> doWork() {
		startTime = Instant.now();
		List<AiTask> childTasks = new ArrayList<>();

		try {
			for (int i = 0; i < config.getDelay(); i++) {
				logger.info(taskName() + ": Sleeping... " + i);
				Thread.sleep(Duration.ofSeconds(1));
			}
		} catch (InterruptedException e) {
			logger.error("Sleep interrupted.");
		}

		for (int i = 0; i < 10; i++) {
			int ticketId = new Random().nextInt(100);
			childTasks.add(new TestChildTask(ticketId));
		}

		return childTasks;
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> result = new HashMap<>();
		result.put("planName", "I slept for " + config.getDelay() + " seconds!");
		return result;
	}

	@Override
	public String getResultTemplateFilename() {
		return "plan-evaluation-template.pug";
	}

}
