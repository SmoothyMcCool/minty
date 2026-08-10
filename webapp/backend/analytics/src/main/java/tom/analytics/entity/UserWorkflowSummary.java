package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserWorkflowSummary")
public class UserWorkflowSummary {
	@EmbeddedId
	private UserWorkflowSummaryId id;

	@Column(name = "workflowName")
	private String workflowName;

	@Column(name = "creationCount")
	private long creationCount;

	@Column(name = "executionCount")
	private long executionCount;

	@Column(name = "createdAt")
	private LocalDateTime createdAt;

	@Column(name = "lastExecutedAt")
	private LocalDateTime lastExecutedAt;

	public UserWorkflowSummary() {
	}

	public UserWorkflowSummaryId getId() {
		return id;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public UUID getWorkflowId() {
		return id != null ? id.getWorkflowId() : null;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public long getCreationCount() {
		return creationCount;
	}

	public long getExecutionCount() {
		return executionCount;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getLastExecutedAt() {
		return lastExecutedAt;
	}
}