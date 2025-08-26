package tom.conversation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.conversation.model.Conversation;

@Service
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	Conversation findByConversationId(UUID conversationId);

	List<Conversation> findAllByOwnerIdAndAssociatedAssistantId(UUID userId, UUID assistantId);

	List<Conversation> findAllByOwnerId(UUID userId);

	void deleteByConversationId(UUID conversationId);

	List<Conversation> findAllByTitle(String string);

}
