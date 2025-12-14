package tom.api.services;

import java.util.List;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.conversation.model.Conversation;
import tom.api.model.ChatMessage;

public interface ConversationService {

	Conversation getConversation(UserId userId, ConversationId conversationId);

	AssistantId getAssistantIdFromConversationId(UserId userId, ConversationId conversationId);

	List<ChatMessage> getChatMessages(UserId userId, ConversationId conversationId);
}
