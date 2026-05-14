package tom.meta.model;

import java.time.Instant;

import tom.api.ConversationId;
import tom.api.UserId;

public record RequestSummary(LlmRequestId llmRequestId, UserId userId, ConversationId conversationId,
		RequestStatus status, Instant createdAt, Instant queuedAt, Instant dequeuedAt, Instant firstTokenAt,
		Instant completedAt, String error, Double queueWaitMs, Double ttftMs, Double totalTimeMs) {
}