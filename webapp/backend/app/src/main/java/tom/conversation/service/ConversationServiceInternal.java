package tom.conversation.service;

import java.util.List;

import tom.task.services.ConversationService;

public interface ConversationServiceInternal extends ConversationService {

	void deleteConversationsForAssistant(int userId, int assistantId);

	String getDefaultConversationId(String username);

	String newConversationId(String username, int assistantId);

	String getUserNameFromConversationId(String conversationId);

	boolean conversationOwnedBy(String conversationId, String username);

	String newConversationId(int userId, int assistantId, String workflowName);

	List<List<String>> getConversationsForWorkflow(String workflowName);

	void deleteConversationsForWorkflow(int userId, String workflowName);
}
