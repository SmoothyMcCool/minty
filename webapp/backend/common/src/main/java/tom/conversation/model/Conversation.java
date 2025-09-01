package tom.conversation.model;

import java.util.UUID;

public class Conversation {

	private String title;
	private UUID conversationId;
	private UUID ownerId;
	private UUID associatedAssistantId;

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
