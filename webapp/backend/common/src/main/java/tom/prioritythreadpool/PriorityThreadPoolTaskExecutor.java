package tom.prioritythreadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import tom.api.ConversationId;

public class PriorityThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

	private static final long serialVersionUID = 1507593744822780926L;

	private final ConcurrentHashMap<ConversationId, PriorityTask> pendingTasks = new ConcurrentHashMap<>();

	@Override
	protected BlockingQueue<Runnable> createQueue(int capacity) {
		// The capacity argument is ignored by PriorityBlockingQueue
		return new PriorityBlockingQueue<>();
	}

	public void execute(Runnable task, ConversationId conversationId, TaskPriority priority) {
		PriorityTask priorityTask = new PriorityTask(task, conversationId, priority);
		pendingTasks.put(conversationId, priorityTask);
		super.execute(priorityTask);
	}

	public boolean cancel(ConversationId conversationId) {
		PriorityTask task = pendingTasks.remove(conversationId);
		if (task != null) {
			task.cancel(true);
			return true;
		}
		return false;
	}

	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (r instanceof PriorityTask pt) {
			pendingTasks.remove(pt.getConversationId());
		}
	}
}
