package tom.assistant.service.management;

import tom.conversation.service.ConversationServiceInternal;
import tom.model.Assistant;
import tom.task.services.assistant.AssistantManagementService;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	Assistant unrestrictedFindAssistant(Integer assistantId);

	void setConversationService(ConversationServiceInternal conversationService);
}
