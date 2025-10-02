package tom.api.services;

import java.util.List;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.model.ChatMessage;

public interface ConversationService {

	AssistantId getAssistantIdFromConversationId(UserId userId, ConversationId conversationId);

	List<ChatMessage> getChatMessages(UserId userId, ConversationId conversationId);
}
