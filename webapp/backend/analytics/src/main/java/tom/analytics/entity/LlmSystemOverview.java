package tom.analytics.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "LlmSystemOverview")
public class LlmSystemOverview {
	@Id
	@Column(name = "id")
	private Integer id;

	private long requests;
	private long completedRequests;
	private long failedRequests;

	private Double avgTTFTMs;
	private Double avgLatencyMs;

	private Long totalTokens;

	public LlmSystemOverview() {
	}

	public Integer getId() {
		return id;
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

	public Double getAvgLatencyMs() {
		return avgLatencyMs;
	}

	public Long getTotalTokens() {
		return totalTokens;
	}
}
