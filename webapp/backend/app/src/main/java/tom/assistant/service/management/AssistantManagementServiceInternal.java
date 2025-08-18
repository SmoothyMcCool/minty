package tom.assistant.service.management;

import tom.api.services.assistant.AssistantManagementService;
import tom.conversation.service.ConversationServiceInternal;
import tom.model.Assistant;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	public static final int WorkflowDefaultAssistantId = -1;

	Assistant unrestrictedFindAssistant(Integer assistantId);

	void setConversationService(ConversationServiceInternal conversationService);
}
