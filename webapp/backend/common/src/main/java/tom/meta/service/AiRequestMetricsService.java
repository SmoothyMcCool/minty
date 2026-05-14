package tom.meta.service;

import java.time.LocalDate;
import java.util.List;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.meta.model.DailyMetricsSummary;
import tom.meta.model.LlmRequestId;
import tom.meta.model.RequestSummary;

/**
 * Public contract for the AI request metrics lifecycle and reporting.
 *
 * <p>
 * Lives in the domain library so any consumer (web layer, messaging, scheduled
 * jobs, etc.) can depend on this interface without pulling in persistence or
 * Spring Data dependencies. The implementation lives in the persistence library
 * and is injected at runtime.
 *
 * <h2>State machine</h2>
 * 
 * <pre>
 *   [start] -> QUEUED -> PROCESSING -> COMPLETED
 *                     -> FAILED
 * </pre>
 *
 * <h2>Typical call sequence for a streaming request</h2>
 * 
 * <pre>{@code
 * RequestSummary req = service.registerRequest(userId, sessionId, model);
 * String id = req.id();
 *
 * service.markDequeued(id); // worker picks up the request
 * service.recordFirstToken(id); // first streamed chunk arrives
 * service.markCompleted(id); // stream fully received
 * service.recordTokenCounts(id, promptTokens, completionTokens);
 * }</pre>
 */
public interface AiRequestMetricsService {

	// ====================================================================
	// Lifecycle - state-change methods
	// ====================================================================

	/**
	 * Creates a new AI request in QUEUED state.
	 *
	 * @param userId         the ID of the user making the request
	 * @param conversationId The conversation used
	 * @return a summary of the newly created request, including its generated ID
	 */
	LlmRequestId registerRequest(UserId userId, ConversationId conversationId);

	/**
	 * Transitions the request from QUEUED → PROCESSING and records the dequeue
	 * timestamp. Call this the moment a worker thread picks the request up and is
	 * about to invoke the upstream model.
	 *
	 * @param requestId the request to transition
	 * @return updated summary
	 * @throws RequestNotFoundException        if the ID does not exist
	 * @throws InvalidStateTransitionException if the request is not currently
	 *                                         QUEUED
	 */
	RequestSummary markDequeued(LlmRequestId llmRequestId);

	/**
	 * Records the timestamp of the first streamed token. The request must be in
	 * PROCESSING state. Status does not change. This method is idempotent -
	 * duplicate calls (common in streaming) are silently ignored.
	 *
	 * @param requestId the request that received its first token
	 * @return updated summary ({@code ttftMs} will now be populated)
	 * @throws RequestNotFoundException        if the ID does not exist
	 * @throws InvalidStateTransitionException if the request is not PROCESSING
	 */
	RequestSummary recordFirstToken(LlmRequestId llmRequestId);

	/**
	 * Transitions the request from PROCESSING → COMPLETED and records
	 * {@code completed_at}. The database trigger populates duration columns in
	 * {@code request_metrics} automatically.
	 *
	 * @param requestId the request that finished successfully
	 * @return updated summary ({@code totalTimeMs} will now be populated)
	 * @throws RequestNotFoundException        if the ID does not exist
	 * @throws InvalidStateTransitionException if the request is not PROCESSING
	 */
	RequestSummary markCompleted(LlmRequestId llmRequestId);

	/**
	 * Transitions the request to FAILED state and records the error message. May be
	 * called from either QUEUED or PROCESSING.
	 *
	 * @param conversationId the request that failed
	 * @param error          a description of the failure
	 * @return updated summary
	 * @throws RequestNotFoundException        if the ID does not exist
	 * @throws InvalidStateTransitionException if the request is already COMPLETED
	 *                                         or FAILED
	 */
	RequestSummary markFailed(LlmRequestId llmRequestId, String error);

	/**
	 * Stores prompt and completion token counts once the full response has been
	 * received. Separate from {@link #markCompleted} because token counts are often
	 * not known until the stream fully drains.
	 *
	 * <p>
	 * The request must be COMPLETED before calling this.
	 *
	 * @param requestId        the completed request
	 * @param promptTokens     number of tokens in the prompt / input
	 * @param completionTokens number of tokens in the model's response
	 * @throws RequestNotFoundException        if the ID does not exist
	 * @throws InvalidStateTransitionException if the request is not COMPLETED
	 * @throws IllegalStateException           if the metrics row is missing
	 *                                         (trigger not installed)
	 */
	void recordTokenCounts(LlmRequestId llmRequestId, int promptTokens, int completionTokens);

	// ====================================================================
	// Metrics - query methods
	// ====================================================================

	/**
	 * Returns a summary of a single request.
	 *
	 * @throws RequestNotFoundException if the ID does not exist
	 */
	RequestSummary getRequest(LlmRequestId llmRequestId);

	/**
	 * Returns all requests for a given user, unordered.
	 */
	List<RequestSummary> getRequestsForUser(UserId userId);

	/**
	 * Returns all active requests for a given user and conversation, unordered.
	 */
	List<RequestSummary> getActiveRequestsForUserAndConversation(UserId userId, ConversationId conversationId);

	/**
	 * Returns all requests belonging to a session.
	 */
	List<RequestSummary> getRequestsForConversation(ConversationId conversationId);

	/**
	 * Returns all requests currently in QUEUED state, oldest first. Useful for
	 * monitoring queue depth.
	 */
	List<RequestSummary> getQueuedRequests();

	/**
	 * Returns p50/p95 daily metrics for all models on a specific day.
	 */
	List<DailyMetricsSummary> getDailyMetrics(LocalDate day);

	/**
	 * Returns p50/p95 daily metrics for all models over a date range, newest first.
	 */
	List<DailyMetricsSummary> getDailyMetrics(LocalDate from, LocalDate to);

	/**
	 * Convenience method returning today's metrics for all models.
	 */
	List<DailyMetricsSummary> getTodayMetrics();

	/**
	 * Returns a rolling window of the last {@code days} days (inclusive of today),
	 * all models, newest first.
	 *
	 * @param days number of days to include, e.g. {@code 7} or {@code 30}
	 */
	List<DailyMetricsSummary> getLastDaysMetrics(int days);

	/**
	 * Returns completed requests that are missing a {@code first_token_at}
	 * timestamp. These indicate a data quality issue - the streaming handler likely
	 * failed to call {@link #recordFirstToken} before {@link #markCompleted}.
	 */
	List<RequestSummary> findCompletedWithoutFirstToken();

}