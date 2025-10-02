package tom.conversation.model;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;

public class Conversation {

	private String title;
	private ConversationId conversationId;
	private UserId ownerId;
	private AssistantId associatedAssistantId;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ConversationId getConversationId() {
		return conversationId;
	}

	public void setConversationId(ConversationId conversationId) {
		this.conversationId = conversationId;
	}

	public UserId getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(UserId ownerId) {
		this.ownerId = ownerId;
	}

	public AssistantId getAssociatedAssistantId() {
		return associatedAssistantId;
	}

	public void setAssociatedAssistantId(AssistantId associatedAssistantId) {
		this.associatedAssistantId = associatedAssistantId;
	}

}
