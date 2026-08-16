package tom.analytics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tom.analytics.entity.AssistantDailyUsage;
import tom.analytics.entity.AssistantLeaderboard;
import tom.analytics.entity.AssistantPopularity;
import tom.analytics.entity.ConversationStatistics;
import tom.analytics.entity.DailySystemStatistics;
import tom.analytics.entity.LlmAssistantMetrics;
import tom.analytics.entity.LlmDailyMetrics;
import tom.analytics.entity.LlmHourlyMetrics;
import tom.analytics.entity.LlmSystemOverview;
import tom.analytics.entity.LlmUserMetrics;
import tom.analytics.entity.SystemOverview;
import tom.analytics.entity.UserAction;
import tom.analytics.entity.UserActionSummary;
import tom.analytics.entity.UserActionType;
import tom.analytics.entity.UserActivitySummary;
import tom.analytics.entity.UserAssistantSummary;
import tom.analytics.entity.UserLoginSummary;
import tom.analytics.entity.UserWorkflowSummary;
import tom.analytics.entity.WorkflowPopularity;
import tom.analytics.repository.AssistantDailyUsageRepository;
import tom.analytics.repository.AssistantLeaderboardRepository;
import tom.analytics.repository.AssistantPopularityRepository;
import tom.analytics.repository.ConversationStatisticsRepository;
import tom.analytics.repository.DailySystemStatisticsRepository;
import tom.analytics.repository.LlmAssistantMetricsRepository;
import tom.analytics.repository.LlmDailyMetricsRepository;
import tom.analytics.repository.LlmHourlyMetricsRepository;
import tom.analytics.repository.LlmSystemOverviewRepository;
import tom.analytics.repository.LlmUserMetricsRepository;
import tom.analytics.repository.SystemOverviewRepository;
import tom.analytics.repository.UserActionRepository;
import tom.analytics.repository.UserActionSummaryRepository;
import tom.analytics.repository.UserActivitySummaryRepository;
import tom.analytics.repository.UserAssistantSummaryRepository;
import tom.analytics.repository.UserLoginSummaryRepository;
import tom.analytics.repository.UserWorkflowSummaryRepository;
import tom.analytics.repository.WorkflowPopularityRepository;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.WorkflowId;
import tom.api.services.ConversationService;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

	private final ConversationStatisticsRepository conversationStatisticsRepository;
	private final ConversationService conversationService;

	private final UserLoginSummaryRepository userLoginSummaryRepository;
	private final UserActivitySummaryRepository userActivitySummaryRepository;
	private final UserAssistantSummaryRepository userAssistantSummaryRepository;

	private final UserActionRepository userActionRepository;
	private final UserActionSummaryRepository userActionSummaryRepository;
	private final UserWorkflowSummaryRepository userWorkflowSummaryRepository;
	private final WorkflowPopularityRepository workflowPopularityRepository;

	private final AssistantPopularityRepository assistantPopularityRepository;
	private final AssistantDailyUsageRepository assistantDailyUsageRepository;
	private final AssistantLeaderboardRepository assistantLeaderboardRepository;

	private final SystemOverviewRepository systemOverviewRepository;
	private final DailySystemStatisticsRepository dailySystemStatisticsRepository;

	private final LlmDailyMetricsRepository llmDailyMetricsRepository;
	private final LlmHourlyMetricsRepository llmHourlyMetricsRepository;
	private final LlmUserMetricsRepository llmUserMetricsRepository;
	private final LlmAssistantMetricsRepository llmAssistantMetricsRepository;
	private final LlmSystemOverviewRepository llmSystemOverviewRepository;

	public AnalyticsServiceImpl(ConversationStatisticsRepository conversationStatisticsRepository,
			ConversationService conversationService, UserLoginSummaryRepository userLoginSummaryRepository,
			UserActivitySummaryRepository userActivitySummaryRepository,
			UserAssistantSummaryRepository userAssistantSummaryRepository, UserActionRepository userActionRepository,
			UserActionSummaryRepository userActionSummaryRepository,
			UserWorkflowSummaryRepository userWorkflowSummaryRepository,
			WorkflowPopularityRepository workflowPopularityRepository,
			AssistantPopularityRepository assistantPopularityRepository,
			AssistantDailyUsageRepository assistantDailyUsageRepository,
			AssistantLeaderboardRepository assistantLeaderboardRepository,
			SystemOverviewRepository systemOverviewRepository,
			DailySystemStatisticsRepository dailySystemStatisticsRepository,
			LlmDailyMetricsRepository llmDailyMetricsRepository, LlmHourlyMetricsRepository llmHourlyMetricsRepository,
			LlmUserMetricsRepository llmUserMetricsRepository,
			LlmAssistantMetricsRepository llmAssistantMetricsRepository,
			LlmSystemOverviewRepository llmSystemOverviewRepository) {

		this.conversationStatisticsRepository = Objects.requireNonNull(conversationStatisticsRepository,
				"conversationStatisticsRepository");

		this.conversationService = Objects.requireNonNull(conversationService, "conversationService");

		this.userLoginSummaryRepository = Objects.requireNonNull(userLoginSummaryRepository,
				"userLoginSummaryRepository");

		this.userActivitySummaryRepository = Objects.requireNonNull(userActivitySummaryRepository,
				"userActivitySummaryRepository");

		this.userAssistantSummaryRepository = Objects.requireNonNull(userAssistantSummaryRepository,
				"userAssistantSummaryRepository");

		this.userActionRepository = Objects.requireNonNull(userActionRepository, "userActionRepository");

		this.userActionSummaryRepository = Objects.requireNonNull(userActionSummaryRepository,
				"userActionSummaryRepository");

		this.userWorkflowSummaryRepository = Objects.requireNonNull(userWorkflowSummaryRepository,
				"userWorkflowSummaryRepository");

		this.workflowPopularityRepository = Objects.requireNonNull(workflowPopularityRepository,
				"workflowPopularityRepository");

		this.assistantPopularityRepository = Objects.requireNonNull(assistantPopularityRepository,
				"assistantPopularityRepository");

		this.assistantDailyUsageRepository = Objects.requireNonNull(assistantDailyUsageRepository,
				"assistantDailyUsageRepository");

		this.assistantLeaderboardRepository = Objects.requireNonNull(assistantLeaderboardRepository,
				"assistantLeaderboardRepository");

		this.systemOverviewRepository = Objects.requireNonNull(systemOverviewRepository, "systemOverviewRepository");

		this.dailySystemStatisticsRepository = Objects.requireNonNull(dailySystemStatisticsRepository,
				"dailySystemStatisticsRepository");

		this.llmDailyMetricsRepository = Objects.requireNonNull(llmDailyMetricsRepository, "llmDailyMetricsRepository");

		this.llmHourlyMetricsRepository = Objects.requireNonNull(llmHourlyMetricsRepository,
				"llmHourlyMetricsRepository");

		this.llmUserMetricsRepository = Objects.requireNonNull(llmUserMetricsRepository, "llmUserMetricsRepository");

		this.llmAssistantMetricsRepository = Objects.requireNonNull(llmAssistantMetricsRepository,
				"llmAssistantMetricsRepository");

		this.llmSystemOverviewRepository = Objects.requireNonNull(llmSystemOverviewRepository,
				"llmSystemOverviewRepository");
	}

	/*
	 * -------------------------------------------------------------------------
	 * Event recording
	 * -------------------------------------------------------------------------
	 */

	private void recordUserAction(UserId userId, UserActionType actionType, ConversationId conversationId,
			AssistantId assistantId, WorkflowId workflowId, LocalDateTime occurredAt, String metadata) {

		requireUserId(userId);

		Objects.requireNonNull(actionType, "actionType");
		Objects.requireNonNull(occurredAt, "occurredAt");

		UserAction action = new UserAction();

		action.setUserId(userId.value());
		action.setActionType(actionType);

		if (conversationId != null) {
			action.setConversationId(conversationId.value());
		}

		if (assistantId != null) {
			action.setAssistantId(assistantId.value());
		}

		if (workflowId != null) {
			action.setWorkflowId(workflowId.value());
		}

		action.setOccurredAt(occurredAt);
		action.setMetadata(metadata);

		userActionRepository.save(action);
	}

	@Override
	@Transactional
	public void recordUserLogin(UserId userId) {
		recordUserLogin(userId, LocalDateTime.now());
	}

	@Override
	@Transactional
	public void recordUserLogin(UserId userId, LocalDateTime loginTime) {
		recordUserAction(userId, UserActionType.UserLoggedIn, null, null, null, loginTime, null);
	}

	@Override
	@Transactional
	public void recordAssistantCreated(UserId userId, AssistantId assistantId) {
		Objects.requireNonNull(assistantId, "assistantId");
		recordUserAction(userId, UserActionType.AssistantCreated, null, assistantId, null, LocalDateTime.now(), null);
	}

	@Override
	@Transactional
	public void recordWorkflowCreated(UserId userId, WorkflowId workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");
		recordUserAction(userId, UserActionType.WorkflowCreated, null, null, workflowId, LocalDateTime.now(), null);
	}

	@Override
	@Transactional
	public void recordWorkflowExecuted(UserId userId, WorkflowId workflowId) {
		Objects.requireNonNull(workflowId, "workflowId");
		recordUserAction(userId, UserActionType.WorkflowExecuted, null, null, workflowId, LocalDateTime.now(), null);
	}

	@Override
	@Transactional
	public ConversationStatistics startConversation(UserId userId, ConversationId conversationId) {
		return startConversation(userId, conversationId, LocalDateTime.now());
	}

	@Override
	@Transactional
	public ConversationStatistics startConversation(UserId userId, ConversationId conversationId,
			LocalDateTime started) {
		requireUserId(userId);
		Objects.requireNonNull(conversationId, "conversation");
		Objects.requireNonNull(started, "started");

		ConversationStatistics existing = conversationStatisticsRepository.findById(conversationId.value())
				.orElse(null);

		if (existing != null) {
			return existing;
		}

		AssistantId assistantId = conversationService.getAssistantIdFromConversationId(userId, conversationId);

		if (assistantId == null) {
			throw new AnalyticsNotFoundException("Conversation " + conversationId + " has no associated assistant");
		}

		ConversationStatistics statistics = new ConversationStatistics();

		statistics.setConversationId(conversationId.value());
		statistics.setUserId(userId);
		statistics.setAssistantId(assistantId);
		statistics.setStarted(started);
		statistics.setLastActivity(started);
		statistics.setCompleted(null);
		statistics.setMessageCount(0);

		ConversationStatistics saved = conversationStatisticsRepository.save(statistics);

		recordUserAction(userId, UserActionType.ConversationStarted, conversationId, assistantId, null, started, null);

		return saved;
	}

	@Override
	@Transactional
	public ConversationStatistics recordMessage(UserId userId, ConversationId conversationId) {
		return recordMessage(userId, conversationId, LocalDateTime.now());
	}

	@Override
	@Transactional
	public ConversationStatistics recordMessage(UserId userId, ConversationId conversationId,
			LocalDateTime activityTime) {
		ConversationStatistics statistics = getConversationStatisticsForUser(userId, conversationId);

		Objects.requireNonNull(activityTime, "activityTime");

		statistics.setMessageCount(statistics.getMessageCount() + 1);
		statistics.setLastActivity(activityTime);

		ConversationStatistics saved = conversationStatisticsRepository.save(statistics);

		recordUserAction(userId, UserActionType.MessageSent, conversationId, statistics.getAssistantId(), null,
				activityTime, null);

		return saved;
	}

	@Override
	@Transactional
	public ConversationStatistics updateConversationActivity(UserId userId, ConversationId conversationId,
			LocalDateTime activityTime) {
		ConversationStatistics statistics = getConversationStatisticsForUser(userId, conversationId);

		Objects.requireNonNull(activityTime, "activityTime");

		statistics.setLastActivity(activityTime);

		return conversationStatisticsRepository.save(statistics);
	}

	@Override
	@Transactional
	public ConversationStatistics completeConversation(UserId userId, ConversationId conversationId) {
		return completeConversation(userId, conversationId, LocalDateTime.now());
	}

	@Override
	@Transactional
	public ConversationStatistics completeConversation(UserId userId, ConversationId conversationId,
			LocalDateTime completed) {
		ConversationStatistics statistics = getConversationStatisticsForUser(userId, conversationId);

		Objects.requireNonNull(completed, "completed");

		statistics.setCompleted(completed);

		LocalDateTime lastActivity = statistics.getLastActivity();

		if (lastActivity == null || completed.isAfter(lastActivity)) {
			statistics.setLastActivity(completed);
		}

		return conversationStatisticsRepository.save(statistics);
	}

	/*
	 * -------------------------------------------------------------------------
	 * User reporting
	 * -------------------------------------------------------------------------
	 */

	@Override
	@Transactional(readOnly = true)
	public UserLoginSummary getUserLoginSummary(UUID userId) {
		Objects.requireNonNull(userId, "userId");

		return userLoginSummaryRepository.findById(userId)
				.orElseThrow(() -> new AnalyticsNotFoundException("No login summary exists for user: " + userId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserLoginSummary> getUserLoginSummaries() {
		return userLoginSummaryRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public UserActivitySummary getUserActivity(UUID userId) {
		Objects.requireNonNull(userId, "userId");

		UserActivitySummary summary = userActivitySummaryRepository.findByUserId(userId);

		if (summary == null) {
			throw new AnalyticsNotFoundException("No activity summary exists for user: " + userId);
		}

		return summary;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserActivitySummary> getUserActivity() {
		return userActivitySummaryRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserAssistantSummary> getUserAssistantUsage(UUID userId) {
		Objects.requireNonNull(userId, "userId");

		return userAssistantSummaryRepository.findByIdUserId(userId);
	}

	/*
	 * -------------------------------------------------------------------------
	 * Assistant reporting
	 * -------------------------------------------------------------------------
	 */

	@Override
	@Transactional(readOnly = true)
	public AssistantPopularity getAssistantPopularity(UUID assistantId) {
		Objects.requireNonNull(assistantId, "assistantId");

		AssistantPopularity popularity = assistantPopularityRepository.findById(assistantId);

		if (popularity == null) {
			throw new AnalyticsNotFoundException("No assistant popularity exists for assistant: " + assistantId);
		}

		return popularity;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AssistantPopularity> getAssistantPopularity() {
		return assistantPopularityRepository.findAllByOrderByConversationCountDesc();
	}

	@Override
	@Transactional(readOnly = true)
	public List<AssistantDailyUsage> getAssistantDailyUsage(LocalDate from, LocalDate to) {
		validateDateRange(from, to);
		return assistantDailyUsageRepository.findByIdDayBetween(from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AssistantDailyUsage> getAssistantDailyUsage(UUID assistantId, LocalDate from, LocalDate to) {
		Objects.requireNonNull(assistantId, "assistantId");
		validateDateRange(from, to);
		return assistantDailyUsageRepository.findByIdAssistantIdAndIdDayBetween(assistantId, from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<AssistantLeaderboard> getAssistantLeaderboard() {
		return assistantLeaderboardRepository.findAllByOrderByRankingAsc();
	}

	/*
	 * -------------------------------------------------------------------------
	 * User action reporting
	 * -------------------------------------------------------------------------
	 */

	@Override
	@Transactional(readOnly = true)
	public UserActionSummary getUserActionSummary(UUID userId) {
		return userActionSummaryRepository.findByUserId(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserActionSummary> getUserActionSummaries() {
		return userActionSummaryRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserWorkflowSummary> getUserWorkflowSummary(UUID userId) {
		return userWorkflowSummaryRepository.findByIdUserIdOrderByExecutionCountDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserWorkflowSummary> getUserWorkflowSummaryByWorkflow(UUID workflowId) {
		return userWorkflowSummaryRepository.findByIdWorkflowId(workflowId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserWorkflowSummary> getMostUsedWorkflowsByUser(UUID userId) {
		return userWorkflowSummaryRepository.findByIdUserIdOrderByExecutionCountDesc(userId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserWorkflowSummary> getMostUsedWorkflows() {
		return userWorkflowSummaryRepository.findAllByOrderByExecutionCountDesc();
	}

	@Override
	@Transactional(readOnly = true)
	public WorkflowPopularity getWorkflowPopularity(UUID workflowId) {
		return workflowPopularityRepository.findByWorkflowId(workflowId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkflowPopularity> getWorkflowPopularity() {
		return workflowPopularityRepository.findAll();
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkflowPopularity> getMostRunWorkflows() {
		return workflowPopularityRepository.findAllByOrderByExecutionCountDesc();
	}

	/*
	 * -------------------------------------------------------------------------
	 * System reporting
	 * -------------------------------------------------------------------------
	 */

	@Override
	@Transactional(readOnly = true)
	public SystemOverview getSystemOverview() {
		return systemOverviewRepository.findFirstBy()
				.orElseThrow(() -> new AnalyticsNotFoundException("System overview is not available"));
	}

	@Override
	@Transactional(readOnly = true)
	public List<DailySystemStatistics> getDailySystemStatistics(LocalDate from, LocalDate to) {
		validateDateRange(from, to);
		return dailySystemStatisticsRepository.findByDayBetweenOrderByDayAsc(from, to);
	}

	/*
	 * ------------------------------------------------------------------------- LLM
	 * reporting
	 * -------------------------------------------------------------------------
	 */

	@Override
	@Transactional(readOnly = true)
	public List<LlmDailyMetrics> getLlmDailyMetrics(LocalDate from, LocalDate to) {
		validateDateRange(from, to);

		return llmDailyMetricsRepository.findByDayBetweenOrderByDayAsc(from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<LlmHourlyMetrics> getLlmHourlyMetrics(LocalDate from, LocalDate to) {
		validateDateRange(from, to);

		return llmHourlyMetricsRepository.findByIdDayBetweenOrderByIdDayAscIdHourAsc(from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public LlmUserMetrics getLlmUserMetrics(UUID userId) {
		Objects.requireNonNull(userId, "userId");

		LlmUserMetrics metrics = llmUserMetricsRepository.findById(userId);

		if (metrics == null) {
			throw new AnalyticsNotFoundException("No LLM metrics exist for user: " + userId);
		}

		return metrics;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LlmUserMetrics> getLlmUserMetrics() {
		return llmUserMetricsRepository.findAllByOrderByRequestsDesc();
	}

	@Override
	@Transactional(readOnly = true)
	public LlmAssistantMetrics getLlmAssistantMetrics(UUID assistantId) {
		Objects.requireNonNull(assistantId, "assistantId");

		LlmAssistantMetrics metrics = llmAssistantMetricsRepository.findById(assistantId);

		if (metrics == null) {
			throw new AnalyticsNotFoundException("No LLM metrics exist for assistant: " + assistantId);
		}

		return metrics;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LlmAssistantMetrics> getLlmAssistantMetrics() {
		return llmAssistantMetricsRepository.findAllByOrderByRequestsDesc();
	}

	@Override
	@Transactional(readOnly = true)
	public LlmSystemOverview getLlmSystemOverview() {
		LlmSystemOverview overview = llmSystemOverviewRepository.findFirstBy();

		if (overview == null) {
			throw new AnalyticsNotFoundException("LLM system overview is not available");
		}

		return overview;
	}

	/*
	 * -------------------------------------------------------------------------
	 * Internal helpers
	 * -------------------------------------------------------------------------
	 */

	private ConversationStatistics getConversationStatisticsForUser(UserId userId, ConversationId conversationId) {
		requireUserId(userId);

		Objects.requireNonNull(conversationId, "conversationId");

		ConversationStatistics statistics = conversationStatisticsRepository.findById(conversationId.value())
				.orElseThrow(() -> new AnalyticsNotFoundException(
						"No conversation statistics exist for conversation: " + conversationId));

		if (!userId.equals(statistics.getUserId())) {
			throw new AnalyticsAccessException(
					"User " + userId + " does not own conversation statistics " + conversationId);
		}

		return statistics;
	}

	private void requireUserId(UserId userId) {
		Objects.requireNonNull(userId, "userId");

		if (userId.value() == null) {
			throw new IllegalArgumentException("userId must contain a value");
		}
	}

	private void validateDateRange(LocalDate from, LocalDate to) {
		Objects.requireNonNull(from, "from");

		Objects.requireNonNull(to, "to");

		if (from.isAfter(to)) {
			throw new IllegalArgumentException("from must not be after to");
		}
	}

	/*
	 * -------------------------------------------------------------------------
	 * Exceptions
	 * -------------------------------------------------------------------------
	 */

	public static class AnalyticsNotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public AnalyticsNotFoundException(String message) {
			super(message);
		}
	}

	public static class AnalyticsAccessException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public AnalyticsAccessException(String message) {
			super(message);
		}
	}

}
