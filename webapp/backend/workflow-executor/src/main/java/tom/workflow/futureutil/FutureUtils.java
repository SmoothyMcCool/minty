package tom.workflow.futureutil;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import tom.api.task.TaskLogger;

public class FutureUtils {

	public static <T> CompletableFuture<Void> allOfFailFast(
			Collection<? extends CompletableFuture<? extends T>> futures, TaskLogger logger) {

		if (futures.isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<Void> result = new CompletableFuture<>();

		AtomicInteger remaining = new AtomicInteger(futures.size());

		for (CompletableFuture<? extends T> f : futures) {
			f.whenComplete((r, ex) -> {
				if (ex != null) {
					logger.warn("Future ended with exception!", ex);
					result.completeExceptionally(ex);
				} else if (remaining.decrementAndGet() == 0) {
					result.complete(null);
				}
			});
		}

		return result;
	}
}
