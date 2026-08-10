package tom.analytics.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "LlmDailyMetrics")
public class LlmDailyMetrics {
	@Id
	@Column(name = "day")
	private LocalDate day;

	private long requests;
	private long completedRequests;
	private long failedRequests;

	private Double avgQueueMs;
	private Double avgTTFTMs;
	private Double avgTotalMs;

	private Double maxQueueMs;
	private Double maxTTFTMs;
	private Double maxTotalMs;

	private Long promptTokens;
	private Long completionTokens;
	private Long totalTokens;

	public LlmDailyMetrics() {
	}

	public LocalDate getDay() {
		return day;
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

	public Double getAvgQueueMs() {
		return avgQueueMs;
	}

	public Double getAvgTTFTMs() {
		return avgTTFTMs;
	}

	public Double getAvgTotalMs() {
		return avgTotalMs;
	}

	public Double getMaxQueueMs() {
		return maxQueueMs;
	}

	public Double getMaxTTFTMs() {
		return maxTTFTMs;
	}

	public Double getMaxTotalMs() {
		return maxTotalMs;
	}

	public Long getPromptTokens() {
		return promptTokens;
	}

	public Long getCompletionTokens() {
		return completionTokens;
	}

	public Long getTotalTokens() {
		return totalTokens;
	}
}
