package tom.meta.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Read-only entity mapping the {@code daily_metrics} database view.
 *
 * <p>
 * Annotated with {@link Immutable} - no inserts or updates are issued against
 * this entity. Use it for reporting queries only.
 *
 * <p>
 * The composite "key" is {@link DailyMetricsId} (day + model).
 */
@Entity
@Table(name = "DailyLlmMetrics")
@Immutable
public class DailyMetrics {

	@EmbeddedId
	private DailyMetricsId id;

	private Long totalRequests;

	private Double avgQueueMs;

	private Double p50QueueMs;

	private Double p95QueueMs;

	private Double avgTtftMs;

	private Double p50TtftMs;

	private Double p95TtftMs;

	private Double avgTotalMs;

	private Double p50TotalMs;

	private Double p95TotalMs;

	protected DailyMetrics() {
	}

	public DailyMetricsId getId() {
		return id;
	}

	public void setId(DailyMetricsId id) {
		this.id = id;
	}

	public Long getTotalRequests() {
		return totalRequests;
	}

	public void setTotalRequests(Long totalRequests) {
		this.totalRequests = totalRequests;
	}

	public Double getAvgQueueMs() {
		return avgQueueMs;
	}

	public void setAvgQueueMs(Double avgQueueMs) {
		this.avgQueueMs = avgQueueMs;
	}

	public Double getP50QueueMs() {
		return p50QueueMs;
	}

	public void setP50QueueMs(Double p50QueueMs) {
		this.p50QueueMs = p50QueueMs;
	}

	public Double getP95QueueMs() {
		return p95QueueMs;
	}

	public void setP95QueueMs(Double p95QueueMs) {
		this.p95QueueMs = p95QueueMs;
	}

	public Double getAvgTtftMs() {
		return avgTtftMs;
	}

	public void setAvgTtftMs(Double avgTtftMs) {
		this.avgTtftMs = avgTtftMs;
	}

	public Double getP50TtftMs() {
		return p50TtftMs;
	}

	public void setP50TtftMs(Double p50TtftMs) {
		this.p50TtftMs = p50TtftMs;
	}

	public Double getP95TtftMs() {
		return p95TtftMs;
	}

	public void setP95TtftMs(Double p95TtftMs) {
		this.p95TtftMs = p95TtftMs;
	}

	public Double getAvgTotalMs() {
		return avgTotalMs;
	}

	public void setAvgTotalMs(Double avgTotalMs) {
		this.avgTotalMs = avgTotalMs;
	}

	public Double getP50TotalMs() {
		return p50TotalMs;
	}

	public void setP50TotalMs(Double p50TotalMs) {
		this.p50TotalMs = p50TotalMs;
	}

	public Double getP95TotalMs() {
		return p95TotalMs;
	}

	public void setP95TotalMs(Double p95TotalMs) {
		this.p95TotalMs = p95TotalMs;
	}

	/**
	 * Produces an immutable {@link DailyMetricsSummary} from this view row.
	 */
	public DailyMetricsSummary toSummary() {
		return new DailyMetricsSummary(id.getDay(), totalRequests == null ? 0L : totalRequests, avgQueueMs, p50QueueMs,
				p95QueueMs, avgTtftMs, p50TtftMs, p95TtftMs, avgTotalMs, p50TotalMs, p95TotalMs);
	}

	@Embeddable
	public static class DailyMetricsId implements Serializable {

		private static final long serialVersionUID = 1632527717174319856L;

		private LocalDate day;

		protected DailyMetricsId() {
		}

		public DailyMetricsId(LocalDate day) {
			this.day = day;
		}

		public LocalDate getDay() {
			return day;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof DailyMetricsId that))
				return false;
			return Objects.equals(day, that.day);
		}

		@Override
		public int hashCode() {
			return Objects.hash(day);
		}
	}
}