package tom.conversation.model;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Conversation {

	private String title;
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID conversationId;
	private UUID ownerId;
	private UUID associatedAssistantId;

	public Conversation() {

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

}
