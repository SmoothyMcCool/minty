package tom.conversation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.conversation.model.ConversationEntity;

@Service
public interface ConversationRepository extends JpaRepository<ConversationEntity, UUID> {

	ConversationEntity findByConversationId(UUID conversationId);

	List<ConversationEntity> findAllByOwnerIdAndAssociatedAssistantId(UUID userId, UUID assistantId);

	List<ConversationEntity> findAllByOwnerId(UUID userId);

	void deleteByConversationId(UUID conversationId);

	List<ConversationEntity> findAllByTitle(String string);

}
