package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserAssistantSummary")
public class UserAssistantSummary {
	@EmbeddedId
	private UserAssistantSummaryId id;

	@Column(name = "account")
	private String account;

	@Column(name = "assistantName")
	private String assistantName;

	@Column(name = "conversationCount")
	private long conversationCount;

	@Column(name = "messageCount")
	private long messageCount;

	@Column(name = "averageMessages")
	private double averageMessages;

	@Column(name = "lastUsed")
	private LocalDateTime lastUsed;

	public UserAssistantSummary() {
	}

	public UserAssistantSummaryId getId() {
		return id;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public UUID getAssistantId() {
		return id != null ? id.getAssistantId() : null;
	}

	public String getAccount() {
		return account;
	}

	public String getAssistantName() {
		return assistantName;
	}

	public long getConversationCount() {
		return conversationCount;
	}

	public long getMessageCount() {
		return messageCount;
	}

	public double getAverageMessages() {
		return averageMessages;
	}

	public LocalDateTime getLastUsed() {
		return lastUsed;
	}
}
