package tom.prioritythreadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class PriorityThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

	private static final long serialVersionUID = 1507593744822780926L;

	@Override
	protected @NonNull BlockingQueue<Runnable> createQueue(int capacity) {
		// The capacity argument is ignored by PriorityBlockingQueue
		return new PriorityBlockingQueue<>();
	}

	public void execute(Runnable task, TaskPriority priority) {
		super.execute(new PriorityTask(task, priority));
	}
}
