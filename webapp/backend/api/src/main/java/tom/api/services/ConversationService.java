package tom.api.services;

import java.util.List;
import java.util.UUID;

import tom.model.ChatMessage;

public interface ConversationService {

	UUID getAssistantIdFromConversationId(UUID userId, UUID conversationId);

	List<ChatMessage> getChatMessages(UUID userId, UUID conversationId);
}
