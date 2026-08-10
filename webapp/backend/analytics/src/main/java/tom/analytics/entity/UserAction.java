package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserAction")
public class UserAction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "userId", nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "actionType", nullable = false, length = 50)
	private UserActionType actionType;

	@Column(name = "conversationId")
	private UUID conversationId;

	@Column(name = "assistantId")
	private UUID assistantId;

	@Column(name = "workflowId")
	private UUID workflowId;

	@Column(name = "occurredAt", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "metadata", columnDefinition = "JSON")
	private String metadata;

	public UserAction() {
	}

	public Long getId() {
		return id;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UserActionType getActionType() {
		return actionType;
	}

	public void setActionType(UserActionType actionType) {
		this.actionType = actionType;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	public UUID getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(UUID workflowId) {
		this.workflowId = workflowId;
	}

	public LocalDateTime getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(LocalDateTime occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}
}