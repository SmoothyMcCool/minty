package tom.analytics.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "AssistantLeaderboard")
public class AssistantLeaderboard {
	@Id
	@Column(name = "ranking")
	private long ranking;

	@Column(name = "id")
	private UUID id;

	@Column(name = "name")
	private String name;

	@Column(name = "conversations")
	private long conversations;

	@Column(name = "messages")
	private long messages;

	@Column(name = "users")
	private long users;

	@Column(name = "averageConversationLength")
	private Double averageConversationLength;

	public AssistantLeaderboard() {
	}

	public long getRanking() {
		return ranking;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getConversations() {
		return conversations;
	}

	public long getMessages() {
		return messages;
	}

	public long getUsers() {
		return users;
	}

	public Double getAverageConversationLength() {
		return averageConversationLength;
	}
}