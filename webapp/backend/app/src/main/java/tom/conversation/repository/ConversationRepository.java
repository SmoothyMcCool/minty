package tom.conversation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import tom.conversation.model.Conversation;

@Service
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

	Conversation findByConversationId(String conversationId);

	List<Conversation> findAllByOwnerIdAndAssociatedAssistantId(int userId, Integer assistantId);

	List<Conversation> findAllByAssociatedWorkflow(String workflowName);

	List<Conversation> findAllByOwnerId(int userId);

	void deleteByConversationId(String conversationId);

	List<Conversation> findAllByTitle(String string);

}
