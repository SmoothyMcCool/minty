package tom.assistant.model.joins;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class UserAssistantId implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 304838745383109854L;

	@Column(name = "userId")
	private UUID userId;

	@Column(name = "assistantId")
	private UUID assistantId;

	public UserAssistantId() {
	}

	public UserAssistantId(UUID userId, UUID assistantId) {
		this.userId = userId;
		this.assistantId = assistantId;
	}

	public UUID getUserId() {
		return userId;
	}

	public UUID getAssistantId() {
		return assistantId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public void setAssistantId(UUID assistantId) {
		this.assistantId = assistantId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof UserAssistantId))
			return false;
		UserAssistantId that = (UserAssistantId) o;
		return Objects.equals(userId, that.userId) && Objects.equals(assistantId, that.assistantId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, assistantId);
	}
}