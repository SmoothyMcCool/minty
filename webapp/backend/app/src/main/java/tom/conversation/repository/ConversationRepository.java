package tom.conversation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.api.AssistantId;
import tom.api.UserId;
import tom.conversation.model.ConversationEntity;

@Service
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

	List<ConversationEntity> findAllByOwnerIdAndAssociatedAssistantId(UserId userId, AssistantId assistantId);

	List<ConversationEntity> findAllByOwnerId(UserId userId);

	List<ConversationEntity> findAllByTitle(String string);

}
