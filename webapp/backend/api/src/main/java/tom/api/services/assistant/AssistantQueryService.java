package tom.api.services.assistant;

import java.util.concurrent.CompletableFuture;

import tom.api.ConversationId;
import tom.api.TaskPriority;
import tom.api.UserId;
import tom.api.model.assistant.AssistantQuery;

public interface AssistantQueryService {

	CompletableFuture<String> ask(UserId userId, AssistantQuery query, TaskPriority priority)
			throws QueueFullException, ConversationInUseException;

	// Only for use by agents unless you like deadlocks.
	String askDirect(UserId userId, AssistantQuery query);

	// Only for use by agents unless you like deadlocks.
	String askStreamingDirect(UserId userId, AssistantQuery query, StreamResult sr);

	ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException;

	ConversationId askStreaming(UserId userId, AssistantQuery query, StreamResult sr) throws QueueFullException;

	LlmResult getResultAndRemoveIfComplete(ConversationId conversationId);

	int getQueuePositionFor(ConversationId streamId);

	LlmResult peekLlmResult(ConversationId conversationId);

	boolean cancelRequest(UserId userId, ConversationId conversationId);

}
