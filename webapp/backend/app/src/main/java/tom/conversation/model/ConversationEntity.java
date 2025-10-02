package tom.conversation.model;

import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.repository.converter.UserIdConverter;

@Entity
@Table(name = "Conversation")
public class ConversationEntity {

	private String title;
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID conversationId;
	@Convert(converter = UserIdConverter.class)
	private UserId ownerId;

	private AssistantId associatedAssistantId;

	public ConversationEntity() {

	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ConversationId getConversationId() {
		return new ConversationId(conversationId);
	}

	public void setConversationId(ConversationId conversationId) {
		this.conversationId = conversationId == null ? null : conversationId.value();
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

	public Conversation fromEntity() {
		Conversation convo = new Conversation();
		convo.setAssociatedAssistantId(associatedAssistantId);
		convo.setConversationId(new ConversationId(conversationId));
		convo.setOwnerId(ownerId);
		convo.setTitle(title);
		return convo;
	}

}
