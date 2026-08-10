package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tom.api.AssistantId;
import tom.api.UserId;

@Entity
@Table(name = "ConversationStatistics")
public class ConversationStatistics {
	@Id
	@Column(name = "conversationId", nullable = false)
	private UUID conversationId;

	@Column
	private UserId userId;

	private AssistantId assistantId;

	@Column(nullable = false)
	private LocalDateTime started;

	@Column(nullable = false)
	private LocalDateTime lastActivity;

	@Column
	private LocalDateTime completed;

	@Column(nullable = false)
	private int messageCount;

	public ConversationStatistics() {
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public UserId getUserId() {
		return userId;
	}

	public void setUserId(UserId userId) {
		this.userId = userId;
	}

	public AssistantId getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(AssistantId assistantId) {
		this.assistantId = assistantId;
	}

	public LocalDateTime getStarted() {
		return started;
	}

	public void setStarted(LocalDateTime started) {
		this.started = started;
	}

	public LocalDateTime getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(LocalDateTime lastActivity) {
		this.lastActivity = lastActivity;
	}

	public LocalDateTime getCompleted() {
		return completed;
	}

	public void setCompleted(LocalDateTime completed) {
		this.completed = completed;
	}

	public int getMessageCount() {
		return messageCount;
	}

	public void setMessageCount(int messageCount) {
		this.messageCount = messageCount;
	}
}
