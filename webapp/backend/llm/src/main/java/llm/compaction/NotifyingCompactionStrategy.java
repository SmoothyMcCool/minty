package llm.compaction;

import java.util.function.Consumer;

import org.springframework.ai.session.compaction.CompactionRequest;
import org.springframework.ai.session.compaction.CompactionResult;
import org.springframework.ai.session.compaction.CompactionStrategy;

public class NotifyingCompactionStrategy implements CompactionStrategy {
	private final CompactionStrategy delegate;
	private final Consumer<String> notifier;

	public NotifyingCompactionStrategy(CompactionStrategy delegate, Consumer<String> notifier) {
		this.delegate = delegate;
		this.notifier = notifier;
	}

	@Override
	public CompactionResult compact(CompactionRequest request) {
		if (notifier != null) {
			notifier.accept("Compacting conversation history...");
		}
		CompactionResult result = delegate.compact(request);
		if (notifier != null) {
			notifier.accept("Compaction complete.");
		}
		return result;
	}
}
