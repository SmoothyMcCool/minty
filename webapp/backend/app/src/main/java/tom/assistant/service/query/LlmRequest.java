package tom.assistant.service.query;

import java.time.Instant;

import tom.api.UserId;
import tom.api.model.AssistantQuery;

public class LlmRequest implements Runnable {

	private final UserId userId;
	private final AssistantQuery query;
	private final Instant queueTime;
	private Runnable task;

	public LlmRequest(UserId userId, AssistantQuery query, Instant queueTime) {
		this.userId = userId;
		this.query = query;
		this.queueTime = queueTime;
	}

	public UserId getUserId() {
		return userId;
	}

	public AssistantQuery getQuery() {
		return query;
	}

	public Instant getQueueTime() {
		return queueTime;
	}

	@Override
	public void run() {
		task.run();
	}

	public void setTask(Runnable task) {
		this.task = task;
	}

}
