package tom.prioritythreadpool;

import java.util.Objects;

import tom.api.ConversationId;

/**
 * A Runnable that knows its priority. Lower numbers = higher priority. The
 * executor will order tasks by this value.
 */
public final class PriorityTask implements Runnable, Comparable<PriorityTask> {

	private final Runnable delegate;
	private final ConversationId conversationId;
	private final TaskPriority priority;

	public PriorityTask(Runnable delegate, ConversationId conversationId, TaskPriority priority) {
		this.delegate = Objects.requireNonNull(delegate);
		this.priority = Objects.requireNonNull(priority);
		this.conversationId = Objects.requireNonNull(conversationId);
	}

	@Override
	public void run() {
		delegate.run();
	}

	/**
	 * Order by the enum's ordinal (High < Medium < Low).
	 */
	@Override
	public int compareTo(PriorityTask other) {
		return this.priority.compareTo(other.priority);
	}

	@Override
	public String toString() {
		return "PriorityTask[" + priority + ", " + delegate + "]";
	}

	public Runnable getDelegate() {
		return delegate;
	}

	public ConversationId getConversationId() {
		return conversationId;
	}

}
