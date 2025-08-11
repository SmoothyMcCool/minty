package tom.conversation.service;

import java.util.List;

import tom.conversation.model.Conversation;
import tom.model.ChatMessage;
import tom.task.services.ConversationService;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(int userId);

	void deleteConversationsForAssistant(int userId, int assistantId);

	Conversation newConversation(int userId, int assistantId);

	Conversation newConversationForWorkflow(int userId, int assistantId, String workflowName);

	String getUserNameFromConversationId(String conversationId);

	boolean conversationOwnedBy(String conversationId, int userId);

	List<List<ChatMessage>> getChatMessagesForWorkflow(String workflowName);

	void deleteConversationsForWorkflow(int userId, String workflowName);

	boolean deleteConversation(int userId, String conversationId);

}
