package tom.analytics.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "LlmUserMetrics")
public class LlmUserMetrics {
	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "account")
	private String account;

	private long requests;
	private long completedRequests;
	private long failedRequests;

	private Long promptTokens;
	private Long completionTokens;
	private Long totalTokens;

	private Double avgTTFTMs;
	private Double avgLatencyMs;

	public LlmUserMetrics() {
	}

	public UUID getId() {
		return id;
	}

	public String getAccount() {
		return account;
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

	public Long getPromptTokens() {
		return promptTokens;
	}

	public Long getCompletionTokens() {
		return completionTokens;
	}

	public Long getTotalTokens() {
		return totalTokens;
	}

	public Double getAvgTTFTMs() {
		return avgTTFTMs;
	}

	public Double getAvgLatencyMs() {
		return avgLatencyMs;
	}
}
