package tom.meta.model;

import java.time.LocalDate;

/**
 * Immutable projection of a single {@code daily_metrics} view row.
 *
 * <p>
 * Lives in the domain library so consumers have no persistence dependency. All
 * durations are in milliseconds. Produced by {@link DailyMetrics#toSummary()}.
 */
public record DailyMetricsSummary(LocalDate day, long totalRequests, Double avgQueueMs, Double p50QueueMs,
		Double p95QueueMs, Double avgTtftMs, Double p50TtftMs, Double p95TtftMs, Double avgTotalMs, Double p50TotalMs,
		Double p95TotalMs) {
}