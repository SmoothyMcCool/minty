package tom.meta.model;

/**
 * Aggregate performance metrics for a single model, expressed in milliseconds.
 *
 * <p>
 * Lives in the domain library so consumers have no persistence dependency. All
 * values are averages across all completed requests for the model. For
 * percentile breakdowns by day use {@code DailyMetricsSummary} instead.
 */
public record ModelMetricsSummary(String model, long totalRequests, Double avgQueueWaitMs, Double avgTtftMs,
		Double avgTotalTimeMs) {
	/** Returns {@code true} when no completed requests exist yet for this model. */
	public boolean isEmpty() {
		return totalRequests == 0;
	}
}