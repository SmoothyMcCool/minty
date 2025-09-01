package tom.conversation.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Conversation")
public class ConversationEntity {

	private String title;
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID conversationId;
	private UUID ownerId;
	private UUID associatedAssistantId;

	public ConversationEntity() {

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public UUID getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UUID ownerId) {
		this.ownerId = ownerId;
	}

	public UUID getAssociatedAssistantId() {
		return associatedAssistantId;
	}

	public void setAssociatedAssistantId(UUID associatedAssistantId) {
		this.associatedAssistantId = associatedAssistantId;
	}

	public Conversation fromEntity() {
		Conversation convo = new Conversation();
		convo.setAssociatedAssistantId(associatedAssistantId);
		convo.setConversationId(conversationId);
		convo.setOwnerId(ownerId);
		convo.setTitle(title);
		return convo;
	}

}
