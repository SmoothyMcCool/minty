package tom.assistant.service;

import tom.conversation.service.ConversationServiceInternal;
import tom.task.services.AssistantService;

public interface AssistantServiceInternal extends AssistantService {

	void setConversationService(ConversationServiceInternal conversationService);

}
