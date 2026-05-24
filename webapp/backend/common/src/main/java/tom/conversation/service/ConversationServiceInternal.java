package tom.conversation.service;

import java.util.List;

import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.conversation.Conversation;
import tom.api.services.ConversationService;

public interface ConversationServiceInternal extends ConversationService {

	List<Conversation> listConversationsForUser(UserId userId);

	void deleteConversationsForAssistant(UserId userId, AssistantId assistantId);

	boolean conversationOwnedBy(UserId userId, ConversationId conversationId);

	boolean deleteConversation(UserId userId, ConversationId conversationId);

	Conversation newConversation(UserId userId, AssistantId assistantId);

	Conversation newConversation(UserId userId, AssistantId assistantId, ProjectId projectId);

	Conversation renameConversation(UserId userId, ConversationId conversationId, String title);

	boolean resetConversation(UserId userId, ConversationId conversationId);

	void updateLastUsed(ConversationId conversationId);

	List<Conversation> listConversationsForProject(UserId userId, ProjectId projectId);

}
