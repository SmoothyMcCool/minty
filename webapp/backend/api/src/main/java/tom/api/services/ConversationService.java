package tom.api.services;

import java.util.List;

import tom.model.ChatMessage;

public interface ConversationService {

	int getAssistantIdFromConversationId(String conversationId);

	List<ChatMessage> getChatMessages(int userId, String conversationId);
}
