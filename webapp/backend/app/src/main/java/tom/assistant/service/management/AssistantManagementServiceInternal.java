package tom.assistant.service.management;

import java.util.UUID;

import tom.api.services.assistant.AssistantManagementService;
import tom.conversation.service.ConversationServiceInternal;
import tom.model.Assistant;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	Assistant unrestrictedFindAssistant(UUID assistantId);

	void setConversationService(ConversationServiceInternal conversationService);
}
