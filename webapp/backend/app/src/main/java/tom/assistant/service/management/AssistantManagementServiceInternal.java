package tom.assistant.service.management;

import tom.api.AssistantId;
import tom.api.UserId;
import tom.api.model.user.ResourceSharingSelection;
import tom.api.model.user.UserSelection;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.conversation.service.ConversationServiceInternal;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	void setConversationService(ConversationServiceInternal conversationService);

	void shareAssistant(UserId userId, ResourceSharingSelection selection) throws NotFoundException, NotOwnedException;

	UserSelection getSharingFor(UserId userId, AssistantId assistantId) throws NotOwnedException, NotFoundException;
}
