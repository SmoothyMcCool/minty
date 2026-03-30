package tom.assistant.service.management;

import tom.api.AssistantId;
import tom.api.UserId;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.exception.NotFoundException;
import tom.api.services.exception.NotOwnedException;
import tom.conversation.service.ConversationServiceInternal;
import tom.user.model.ResourceSharingSelection;
import tom.user.model.UserSelection;

public interface AssistantManagementServiceInternal extends AssistantManagementService {

	void setConversationService(ConversationServiceInternal conversationService);

	void shareAssistant(UserId userId, ResourceSharingSelection selection) throws NotFoundException, NotOwnedException;

	UserSelection getSharingFor(UserId userId, AssistantId assistantId) throws NotOwnedException, NotFoundException;
}
