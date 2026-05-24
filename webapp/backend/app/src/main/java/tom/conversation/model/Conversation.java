package tom.conversation.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.repository.converter.ProjectIdConverter;
import tom.repository.converter.UserIdConverter;

@Entity
@Table(name = "Conversation")
public class Conversation {

	private String title;
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;
	@Convert(converter = UserIdConverter.class)
	private UserId ownerId;
	private AssistantId associatedAssistantId;
	private Instant lastUsed;
	@Convert(converter = ProjectIdConverter.class)
	private ProjectId projectId;

	public Conversation() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public ConversationId getId() {
		return new ConversationId(id);
	}

	public void setId(ConversationId id) {
		this.id = id == null ? null : id.value();
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

	public ProjectId getProjectId() {
		return projectId;
	}

	public void setProjectId(ProjectId projectId) {
		this.projectId = projectId;
	}

	public tom.api.model.conversation.Conversation fromEntity() {
		tom.api.model.conversation.Conversation convo = new tom.api.model.conversation.Conversation();
		convo.setAssociatedAssistantId(associatedAssistantId);
		convo.setId(new ConversationId(id));
		convo.setOwnerId(ownerId);
		convo.setTitle(title);
		convo.setLastUsed(lastUsed);
		convo.setProjectId(projectId);
		return convo;
	}

}
