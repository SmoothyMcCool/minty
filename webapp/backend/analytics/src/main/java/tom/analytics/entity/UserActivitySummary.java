package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "UserActivitySummary")
public class UserActivitySummary {
	@Id
	@Column(name = "userId")
	private UUID userId;

	@Column(name = "account")
	private String account;

	@Column(name = "conversationCount")
	private long conversationCount;

	@Column(name = "messageCount")
	private long messageCount;

	@Column(name = "averageMessages")
	private double averageMessages;

	@Column(name = "lastConversation")
	private LocalDateTime lastConversation;

	@Column(name = "openConversations")
	private long openConversations;

	@Column(name = "completedConversations")
	private long completedConversations;

	public UserActivitySummary() {
	}

	public UUID getUserId() {
		return userId;
	}

	public String getAccount() {
		return account;
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

	public LocalDateTime getLastConversation() {
		return lastConversation;
	}

	public long getOpenConversations() {
		return openConversations;
	}

	public long getCompletedConversations() {
		return completedConversations;
	}
}
