package tom.conversation.service;

import java.util.List;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.services.ConversationService;
import tom.conversation.model.Conversation;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(UserId userId);

	void deleteConversationsForAssistant(UserId userId, AssistantId assistantId);

	boolean conversationOwnedBy(UserId userId, ConversationId conversationId);

	boolean deleteConversation(UserId userId, ConversationId conversationId);

	Conversation newConversation(UserId userId, AssistantId assistantId);

	Conversation renameConversation(UserId userId, ConversationId conversationId, String title);

	boolean resetConversation(UserId userId, ConversationId conversationId);

}
