package tom.analytics.entity;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "AssistantDailyUsage")
public class AssistantDailyUsage {
	@EmbeddedId
	private AssistantDailyUsageId id;

	@Column(name = "name")
	private String name;

	@Column(name = "conversationCount")
	private long conversationCount;

	@Column(name = "messageCount")
	private long messageCount;

	@Column(name = "uniqueUsers")
	private long uniqueUsers;

	public AssistantDailyUsage() {
	}

	public AssistantDailyUsageId getId() {
		return id;
	}

	public LocalDate getDay() {
		return id != null ? id.getDay() : null;
	}

	public UUID getAssistantId() {
		return id != null ? id.getAssistantId() : null;
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
}
