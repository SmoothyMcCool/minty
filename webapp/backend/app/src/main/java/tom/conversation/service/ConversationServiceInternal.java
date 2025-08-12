package tom.conversation.service;

import java.util.List;

import tom.conversation.model.Conversation;
import tom.task.services.ConversationService;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(int userId);

	void deleteConversationsForAssistant(int userId, int assistantId);

	Conversation newConversation(int userId, int assistantId);

	boolean conversationOwnedBy(String conversationId, int userId);

	boolean deleteConversation(int userId, String conversationId);

}
