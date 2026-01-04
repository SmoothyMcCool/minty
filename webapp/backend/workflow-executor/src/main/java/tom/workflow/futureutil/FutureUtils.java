package tom.workflow.futureutil;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import tom.api.task.TaskLogger;

public class FutureUtils {

	public static <T> CompletableFuture<Void> allOfFailFast(
			Collection<? extends CompletableFuture<? extends T>> futures, TaskLogger logger) {

		CompletableFuture<Void> firstFailure = new CompletableFuture<>();

		for (CompletableFuture<? extends T> f : futures) {
			f.whenComplete((r, ex) -> {
				if (ex != null) {
					logger.warn("Future ended with exception!", ex);
					firstFailure.completeExceptionally(ex);
				}
			});
		}

		CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

		return allDone.thenCompose(item -> firstFailure.handle((ignore, ex) -> {
			if (ex != null) {
				throw new CompletionException(ex);
			}
			return null;
		}));
	}
}
