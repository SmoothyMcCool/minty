package tom.analytics.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "LlmHourlyMetrics")
public class LlmHourlyMetrics {
	@EmbeddedId
	private LlmHourlyMetricsId id;

	private long requests;
	private long completedRequests;
	private long failedRequests;

	private Double avgTTFTMs;
	private Double avgTotalMs;

	public LlmHourlyMetrics() {
	}

	public LlmHourlyMetricsId getId() {
		return id;
	}

	public java.time.LocalDate getDay() {
		return id != null ? id.getDay() : null;
	}

	public Integer getHour() {
		return id != null ? id.getHour() : null;
	}

	public long getRequests() {
		return requests;
	}

	public long getCompletedRequests() {
		return completedRequests;
	}

	public long getFailedRequests() {
		return failedRequests;
	}

	public Double getAvgTTFTMs() {
		return avgTTFTMs;
	}

	public Double getAvgTotalMs() {
		return avgTotalMs;
	}
}
