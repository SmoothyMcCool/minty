package tom.assistant.service.management;

import tom.api.AssistantId;
import tom.api.model.Assistant;
import tom.api.services.assistant.AssistantManagementService;
import tom.conversation.service.ConversationServiceInternal;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	Assistant unrestrictedFindAssistant(AssistantId assistantId);

	void setConversationService(ConversationServiceInternal conversationService);
}
