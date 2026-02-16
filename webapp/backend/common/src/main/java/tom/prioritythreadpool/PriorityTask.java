package tom.prioritythreadpool;

import java.time.Instant;
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
	private final Instant submitTime;

	public PriorityTask(Runnable delegate, ConversationId conversationId, TaskPriority priority) {
		this.delegate = Objects.requireNonNull(delegate);
		this.priority = Objects.requireNonNull(priority);
		this.conversationId = Objects.requireNonNull(conversationId);
		submitTime = Instant.now();
	}

	@Override
	public void run() {
		delegate.run();
	}

	/**
	 * Order by the enum's ordinal (High < Medium < Low). On ties, older submissions
	 * are higher priority.
	 */
	@Override
	public int compareTo(PriorityTask other) {
		int priorityComparison = Integer.compare(this.priorityWeight(), other.priorityWeight());
		if (priorityComparison == 0) {
			return this.submitTime.compareTo(other.submitTime);
		}
		return priorityComparison;
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

	private int priorityWeight() {
		return switch (priority) {
		case High -> 0;
		case Medium -> 1;
		case Low -> 2;
		};
	}
}
