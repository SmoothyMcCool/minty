package tom.conversation.service;

import java.util.List;
import java.util.UUID;

import tom.api.services.ConversationService;
import tom.conversation.model.Conversation;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(UUID userId);

	void deleteConversationsForAssistant(UUID userId, UUID assistantId);

	Conversation newConversation(UUID userId, UUID assistantId);

	boolean conversationOwnedBy(UUID conversationId, UUID userId);

	boolean deleteConversation(UUID userId, UUID conversationId);

	Conversation renameConversation(UUID userId, UUID conversationId, String title);

}
