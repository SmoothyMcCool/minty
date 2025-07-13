package tom.task.services;

public interface ConversationService {

	void deleteConversationsForAssistant(int id);

	String getDefaultConversationId(int userId);

}
