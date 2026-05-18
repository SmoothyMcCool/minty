package tom.meta.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tom.api.ConversationId;
import tom.api.UserId;
import tom.meta.model.AiRequest;
import tom.meta.model.DailyMetricsSummary;
import tom.meta.model.LlmRequestId;
import tom.meta.model.RequestStatus;
import tom.meta.model.RequestSummary;
import tom.meta.repository.AiRequestRepository;
import tom.meta.repository.DailyMetricsRepository;
import tom.meta.repository.RequestMetricsRepository;
import tom.meta.repository.RequestStatusRepository;

/**
 * Persistence-backed implementation of {@link AiRequestMetricsService}.
 *
 * <p>
 * Lives in the persistence library. Depends on the domain library (entities,
 * records, interface) but the domain library has no knowledge of this class -
 * the dependency is strictly unidirectional.
 *
 * <p>
 * All public methods delegate state-machine validation to {@link #loadAndGuard}
 * before touching the database.
 */
@Service
@Transactional(readOnly = true)
public class AiRequestMetricsServiceImpl implements AiRequestMetricsService {

	private static final Logger logger = LogManager.getLogger(AiRequestMetricsServiceImpl.class);

	// Valid predecessor states for each lifecycle transition
	private static final Set<RequestStatus> CAN_DEQUEUE = EnumSet.of(RequestStatus.QUEUED);
	private static final Set<RequestStatus> CAN_FIRST_TOK = EnumSet.of(RequestStatus.PROCESSING);
	private static final Set<RequestStatus> CAN_COMPLETE = EnumSet.of(RequestStatus.PROCESSING);
	private static final Set<RequestStatus> CAN_FAIL = EnumSet.of(RequestStatus.QUEUED, RequestStatus.PROCESSING);

	private final AiRequestRepository requestRepository;
	private final RequestMetricsRepository metricsRepository;
	private final RequestStatusRepository statusRepository;
	private final DailyMetricsRepository dailyMetricsRepository;

	public AiRequestMetricsServiceImpl(AiRequestRepository requestRepository,
			RequestMetricsRepository metricsRepository, RequestStatusRepository statusRepository,
			DailyMetricsRepository dailyMetricsRepository) {
		this.requestRepository = requestRepository;
		this.metricsRepository = metricsRepository;
		this.statusRepository = statusRepository;
		this.dailyMetricsRepository = dailyMetricsRepository;
	}

	@Override
	@Transactional
	public LlmRequestId registerRequest(UserId userId, ConversationId conversationId) {
		logger.info("registerRequest " + userId.value() + " - " + conversationId.value());
		AiRequest request = AiRequest.create(userId, conversationId,
				statusRepository.getByStatus(RequestStatus.QUEUED));
		request = requestRepository.save(request);
		logger.debug("Registered request {} for user={}", request, userId);
		return request.getId();
	}

	@Override
	@Transactional
	public RequestSummary markDequeued(LlmRequestId llmRequestId) {
		logger.info("markDequeued " + llmRequestId.value());
		AiRequest request = loadAndGuard(llmRequestId, "markDequeued", CAN_DEQUEUE);
		Instant now = Instant.now();
		requestRepository.markDequeued(llmRequestId.value(), now);
		request.setStatus(statusRepository.getByStatus(RequestStatus.PROCESSING));
		request.setDequeuedAt(now);
		logger.debug("Request {} dequeued at {}", llmRequestId, now);
		return request.toSummary();
	}

	@Override
	@Transactional
	public RequestSummary recordFirstToken(LlmRequestId llmRequestId) {
		logger.info("recordFirstToken " + llmRequestId.value());

		AiRequest request = loadAndGuard(llmRequestId, "recordFirstToken", CAN_FIRST_TOK);
		if (request.getFirstTokenAt() != null) {
			logger.trace("Request {} first token already recorded, ignoring duplicate", llmRequestId);
			return request.toSummary();
		}
		Instant now = Instant.now();
		requestRepository.recordFirstToken(llmRequestId.value(), now);
		request.setFirstTokenAt(now);
		logger.debug("Request {} first token at {}", llmRequestId, now);
		return request.toSummary();
	}

	@Override
	@Transactional
	public RequestSummary markCompleted(LlmRequestId llmRequestId) {
		logger.info("markCompleted " + llmRequestId);

		AiRequest request = loadAndGuard(llmRequestId, "markCompleted", CAN_COMPLETE);
		Instant now = Instant.now();
		requestRepository.markCompleted(llmRequestId.value(), now);
		request.setStatus(statusRepository.getByStatus(RequestStatus.COMPLETED));
		request.setCompletedAt(now);
		logger.info("Request {} completed, total={}ms", llmRequestId, millisBetween(request.getCreatedAt(), now));
		return request.toSummary();
	}

	@Override
	@Transactional
	public RequestSummary markFailed(LlmRequestId llmRequestId, String error) {
		logger.info("markFailed " + llmRequestId.value());
		Optional<AiRequest> maybeRequest = requestRepository.findById(llmRequestId.value());

		if (maybeRequest.isPresent()) {
			AiRequest request = maybeRequest.get();
			Instant now = Instant.now();
			requestRepository.markFailed(llmRequestId.value(), now, error);
			request.setStatus(statusRepository.getByStatus(RequestStatus.FAILED));
			request.setCompletedAt(now);
			request.setError(error);
			logger.warn("Request {} failed: {}", request.getId(), error);
			return request.toSummary();
		}

		return null;
	}

	@Override
	@Transactional
	public void recordTokenCounts(LlmRequestId llmRequestId, int promptTokens, int completionTokens) {
		AiRequest request = load(llmRequestId);
		if (request.getStatusEnum() != RequestStatus.COMPLETED) {
			throw new InvalidStateTransitionException(llmRequestId, request.getStatusEnum(), "recordTokenCounts");
		}
		int updated = metricsRepository.updateTokenCounts(llmRequestId.value(), promptTokens, completionTokens);
		if (updated == 0) {
			throw new IllegalStateException("request_metrics row missing for completed request " + llmRequestId.value()
					+ " - check that the trg_compute_metrics trigger is installed");
		}
		logger.debug("Request {} tokens: prompt={} completion={}", llmRequestId, promptTokens, completionTokens);
	}

	@Override
	public RequestSummary getRequest(LlmRequestId llmRequestId) {
		return load(llmRequestId).toSummary();
	}

	@Override
	public List<RequestSummary> getRequestsForUser(UserId userId) {
		return requestRepository.findByUserId(userId).stream().map(AiRequest::toSummary).toList();
	}

	@Override
	public List<RequestSummary> getRequestsForConversation(ConversationId conversationId) {
		return requestRepository.findByConversationId(conversationId).stream().map(AiRequest::toSummary).toList();
	}

	@Override
	public List<RequestSummary> getActiveRequestsForUserAndConversation(UserId userId, ConversationId conversationId) {
		return requestRepository.findByUserIdAndConversationIdAndCompletedAtIsNull(userId, conversationId).stream()
				.map(AiRequest::toSummary).toList();
	}

	@Override
	public List<RequestSummary> getQueuedRequests() {
		return requestRepository.findByStatusOrderByQueuedAt(RequestStatus.QUEUED.toDbValue()).stream()
				.map(AiRequest::toSummary).toList();
	}

	@Override
	public List<DailyMetricsSummary> getDailyMetrics(LocalDate day) {
		return dailyMetricsRepository.findByDay(day).stream().map(d -> d.toSummary()).toList();
	}

	@Override
	public List<DailyMetricsSummary> getDailyMetrics(LocalDate from, LocalDate to) {
		return dailyMetricsRepository.findByDayBetween(from, to).stream().map(d -> d.toSummary()).toList();
	}

	@Override
	public List<DailyMetricsSummary> getTodayMetrics() {
		return dailyMetricsRepository.findToday().stream().map(d -> d.toSummary()).toList();
	}

	@Override
	public List<DailyMetricsSummary> getLastDaysMetrics(int days) {
		return dailyMetricsRepository.findLastDays(days).stream().map(d -> d.toSummary()).toList();
	}

	@Override
	public List<RequestSummary> findCompletedWithoutFirstToken() {
		return requestRepository.findCompletedWithoutFirstToken().stream().map(AiRequest::toSummary).toList();
	}

	private AiRequest loadAndGuard(LlmRequestId llmRequestId, String operation, Set<RequestStatus> allowedStatuses) {
		AiRequest request = load(llmRequestId);
		RequestStatus current = request.getStatusEnum();
		if (!allowedStatuses.contains(current)) {
			throw new InvalidStateTransitionException(llmRequestId, current, operation);
		}
		return request;
	}

	private AiRequest load(LlmRequestId llmRequestId) {
		return requestRepository.findById(llmRequestId.value())
				.orElseThrow(() -> new RequestNotFoundException(llmRequestId));
	}

	private static long millisBetween(Instant start, Instant end) {
		return end.toEpochMilli() - start.toEpochMilli();
	}

}
