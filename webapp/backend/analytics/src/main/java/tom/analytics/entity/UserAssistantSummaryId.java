package tom.analytics.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserAssistantSummaryId implements Serializable {
	private static final long serialVersionUID = 8426196157921310999L;

	@Column(name = "userId")
	private UUID userId;

	@Column(name = "assistantId")
	private UUID assistantId;

	public UserAssistantSummaryId() {
	}

	public UserAssistantSummaryId(UUID userId, UUID assistantId) {
		this.userId = userId;
		this.assistantId = assistantId;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}

		if (!(object instanceof UserAssistantSummaryId other)) {
			return false;
		}

		return Objects.equals(userId, other.userId) && Objects.equals(assistantId, other.assistantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, assistantId);
	}
}
