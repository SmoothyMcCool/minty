package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "WorkflowPopularity")
public class WorkflowPopularity {
	@Id
	@Column(name = "workflowId")
	private UUID workflowId;

	@Column(name = "workflowName")
	private String workflowName;

	@Column(name = "creators")
	private long creators;

	@Column(name = "creationCount")
	private long creationCount;

	@Column(name = "uniqueUsers")
	private long uniqueUsers;

	@Column(name = "executionCount")
	private long executionCount;

	@Column(name = "firstCreatedAt")
	private LocalDateTime firstCreatedAt;

	@Column(name = "lastExecutedAt")
	private LocalDateTime lastExecutedAt;

	public WorkflowPopularity() {
	}

	public UUID getWorkflowId() {
		return workflowId;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public long getCreators() {
		return creators;
	}

	public long getCreationCount() {
		return creationCount;
	}

	public long getUniqueUsers() {
		return uniqueUsers;
	}

	public long getExecutionCount() {
		return executionCount;
	}

	public LocalDateTime getFirstCreatedAt() {
		return firstCreatedAt;
	}

	public LocalDateTime getLastExecutedAt() {
		return lastExecutedAt;
	}
}