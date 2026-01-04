package tom.api.services;

import java.util.List;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.model.conversation.ChatMessage;
import tom.api.model.conversation.Conversation;

public interface ConversationService {

	Conversation getConversation(UserId userId, ConversationId conversationId);

	AssistantId getAssistantIdFromConversationId(UserId userId, ConversationId conversationId);

	List<ChatMessage> getChatMessages(UserId userId, ConversationId conversationId);
}
