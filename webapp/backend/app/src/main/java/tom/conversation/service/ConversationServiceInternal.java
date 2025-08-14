package tom.conversation.service;

import java.util.List;

import tom.api.services.ConversationService;
import tom.conversation.model.Conversation;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(int userId);

	void deleteConversationsForAssistant(int userId, int assistantId);

	Conversation newConversation(int userId, int assistantId);

	boolean conversationOwnedBy(String conversationId, int userId);

	boolean deleteConversation(int userId, String conversationId);

}
