package tom.api.model.conversation;

import java.time.Instant;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;

public class Conversation {

	private ConversationId id;
	private String title;
	private UserId ownerId;
	private AssistantId associatedAssistantId;
	private Instant lastUsed;
	private ProjectId linkedProject;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ConversationId getId() {
		return id;
	}

	public void setId(ConversationId id) {
		this.id = id;
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

	public Instant getLastUsed() {
		return lastUsed;
	}

	public void setLastUsed(Instant lastUsed) {
		this.lastUsed = lastUsed;
	}

	public ProjectId getProject() {
		return linkedProject;
	}

	public void setProjectId(ProjectId id) {
		linkedProject = id;
	}

}
