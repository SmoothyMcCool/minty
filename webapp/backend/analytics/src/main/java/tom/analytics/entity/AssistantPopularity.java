package tom.analytics.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "AssistantPopularity")
public class AssistantPopularity {
	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "name")
	private String name;

	@Column(name = "conversationCount")
	private long conversationCount;

	@Column(name = "messageCount")
	private long messageCount;

	@Column(name = "uniqueUsers")
	private long uniqueUsers;

	@Column(name = "averageMessages")
	private Double averageMessages;

	@Column(name = "lastUsed")
	private LocalDateTime lastUsed;

	public AssistantPopularity() {
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getConversationCount() {
		return conversationCount;

	}

	public long getMessageCount() {
		return messageCount;
	}

	public long getUniqueUsers() {
		return uniqueUsers;
	}

	public Double getAverageMessages() {
		return averageMessages;
	}

	public LocalDateTime getLastUsed() {
		return lastUsed;
	}
}
