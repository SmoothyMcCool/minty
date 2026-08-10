package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserActionSummary")
public class UserActionSummary {
	@Id
	@Column(name = "userId")
	private UUID userId;

	@Column(name = "assistantCreationCount")
	private long assistantCreationCount;

	@Column(name = "workflowCreationCount")
	private long workflowCreationCount;

	@Column(name = "workflowExecutionCount")
	private long workflowExecutionCount;

	@Column(name = "lastAssistantCreation")
	private LocalDateTime lastAssistantCreation;

	@Column(name = "lastWorkflowCreation")
	private LocalDateTime lastWorkflowCreation;

	@Column(name = "lastWorkflowExecution")
	private LocalDateTime lastWorkflowExecution;

	public UserActionSummary() {
	}

	public UUID getUserId() {
		return userId;
	}

	public long getAssistantCreationCount() {
		return assistantCreationCount;
	}

	public long getWorkflowCreationCount() {
		return workflowCreationCount;
	}

	public long getWorkflowExecutionCount() {
		return workflowExecutionCount;
	}

	public LocalDateTime getLastAssistantCreation() {
		return lastAssistantCreation;
	}

	public LocalDateTime getLastWorkflowCreation() {
		return lastWorkflowCreation;
	}

	public LocalDateTime getLastWorkflowExecution() {
		return lastWorkflowExecution;
	}
}