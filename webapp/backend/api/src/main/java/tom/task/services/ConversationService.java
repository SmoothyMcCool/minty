package tom.task.services;

public interface ConversationService {

	void deleteConversationsForAssistant(int userId, int assistantId);

	String getDefaultConversationId(int userId);

	String newConversationId(int userId, String assistantId);

	String newConversationId(int userId, int assistantId, String conversationId);

	int getAssistantIdFromConversationId(String conversationId);

	String getUserIdFromConversationId(String conversationId);

}
