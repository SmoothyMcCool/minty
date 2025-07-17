package tom.tasks.test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.AiTask;

public class TestChildTask implements AiTask {

	private final Logger logger = LogManager.getLogger(TestChildTask.class);

	private final int ticketId;
	private final Instant startTime;

	public TestChildTask(int ticketId) {
		this.ticketId = ticketId;
		startTime = Instant.now();
	}

	@Override
	public List<AiTask> doWork() {
		try {
			for (int i = 0; i < 3; i++) {
				logger.info(taskName() + ": Sleeping... " + i);
				Thread.sleep(Duration.ofSeconds(1));
			}
		} catch (InterruptedException e) {
			logger.error("Sleep interrupted.");
		}
		return null;
	}

	@Override
	public String taskName() {
		return "TestChildTask-" + ticketId + "-" + startTime.getEpochSecond();
	}

	@Override
	public Map<String, Object> getResult() {
		Map<String, Object> results = new HashMap<>();

		results.put("url", "http://ewmlink");
		results.put("ewmId", ticketId);
		results.put("summary", "Ticket Title from EWM");
		results.put("evaluation", "Ticket " + ticketId + ": imagine if an AI gave this result");

		return results;
	}

	@Override
	public String getResultTemplateFilename() {
		return null;
	}

}
