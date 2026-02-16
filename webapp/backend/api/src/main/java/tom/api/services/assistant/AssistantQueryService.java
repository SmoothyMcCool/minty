package tom.api.services.assistant;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;

public interface AssistantQueryService {

	ConversationId ask(UserId userId, AssistantQuery query) throws QueueFullException, ConversationInUseException;

	ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException;

	LlmResult getResultAndRemoveIfComplete(ConversationId conversationId);

	int getQueuePositionFor(ConversationId streamId);

	LlmResult peekLlmResult(ConversationId conversationId);

}
