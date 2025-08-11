package tom.conversation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Conversation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String title;
	private String conversationId;
	private Integer ownerId;
	private Integer associatedAssistantId;
	private String associatedWorkflow;

	public Conversation() {

	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public Integer getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	public Integer getAssociatedAssistantId() {
		return associatedAssistantId;
	}

	public void setAssociatedAssistantId(Integer associatedAssistantId) {
		this.associatedAssistantId = associatedAssistantId;
	}

	public String getAssociatedWorkflow() {
		return associatedWorkflow;
	}

	public void setAssociatedWorkflow(String associatedWorkflow) {
		this.associatedWorkflow = associatedWorkflow;
	}

	
}
