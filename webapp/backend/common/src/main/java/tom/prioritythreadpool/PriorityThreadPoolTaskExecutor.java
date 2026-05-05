package tom.prioritythreadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import tom.api.ConversationId;

public class PriorityThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

	private static final long serialVersionUID = 1507593744822780926L;

	@Override
	protected BlockingQueue<Runnable> createQueue(int capacity) {
		// The capacity argument is ignored by PriorityBlockingQueue
		return new PriorityBlockingQueue<>();
	}

	public void execute(Runnable task, ConversationId conversationId, TaskPriority priority) {
		super.execute(new PriorityTask(task, conversationId, priority));
	}
}
