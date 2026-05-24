package tom.conversation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tom.api.AssistantId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.conversation.model.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	List<Conversation> findAllByOwnerIdAndAssociatedAssistantId(UserId userId, AssistantId assistantId);

	List<Conversation> findAllByOwnerId(UserId userId);

	List<Conversation> findAllByTitle(String string);

	List<Conversation> findByProjectIdAndOwnerId(ProjectId projectId, UserId userId);

}
