package tom.assistant.model.joins;

import java.util.UUID;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import tom.api.UserId;
import tom.assistant.model.Assistant;

@Entity
@Table(name = "UserAssistantLinks")
public class UserAssistantLink {

	@EmbeddedId
	private UserAssistantId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("assistantId")
	@JoinColumn(name = "assistantId", nullable = false)
	private Assistant assistant;

	public UserAssistantLink() {
	}

	public UserAssistantLink(UserId userId, Assistant assistant) {
		this.assistant = assistant;
		id = new UserAssistantId(userId.getValue(), assistant.getId().getValue());
	}

	public UserAssistantId getId() {
		return id;
	}

	public void setId(UserAssistantId id) {
		this.id = id;
	}

	public UUID getUserId() {
		return id != null ? id.getUserId() : null;
	}

	public Assistant getAssistant() {
		return assistant;
	}

	public void setAssistant(Assistant assistant) {
		this.assistant = assistant;
	}
}
