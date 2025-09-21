package tom.api.services.assistant;

import java.util.UUID;

import tom.model.AssistantQuery;

public interface AssistantQueryService {

	UUID ask(UUID userId, AssistantQuery query) throws QueueFullException;

	UUID askStreaming(UUID userId, AssistantQuery query) throws QueueFullException;

	LlmResult getResultFor(UUID requestId);

	int getQueuePositionFor(UUID streamId);

}
