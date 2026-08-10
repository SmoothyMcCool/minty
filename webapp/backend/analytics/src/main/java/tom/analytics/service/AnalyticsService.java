package tom.analytics.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
import tom.analytics.entity.UserActionSummary;
import tom.analytics.entity.UserActivitySummary;
import tom.analytics.entity.UserAssistantSummary;
import tom.analytics.entity.UserLoginSummary;
import tom.analytics.entity.UserWorkflowSummary;
import tom.analytics.entity.WorkflowPopularity;
import tom.api.AssistantId;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.WorkflowId;

public interface AnalyticsService {
	/*
	 * -------------------------------------------------------------------------
	 * Event recording
	 * -------------------------------------------------------------------------
	 */

	void recordUserLogin(UserId userId);

	void recordUserLogin(UserId userId, LocalDateTime loginTime);

	ConversationStatistics startConversation(UserId userId, ConversationId conversationId);

	ConversationStatistics startConversation(UserId userId, ConversationId conversationId, LocalDateTime started);

	ConversationStatistics recordMessage(UserId userId, ConversationId conversationId);

	ConversationStatistics recordMessage(UserId userId, ConversationId conversationId, LocalDateTime activityTime);

	ConversationStatistics updateConversationActivity(UserId userId, ConversationId conversationId,
			LocalDateTime activityTime);

	ConversationStatistics completeConversation(UserId userId, ConversationId conversationId);

	ConversationStatistics completeConversation(UserId userId, ConversationId conversationId, LocalDateTime completed);

	void recordAssistantCreated(UserId userId, AssistantId assistantId);

	void recordWorkflowCreated(UserId userId, WorkflowId workflowId);

	void recordWorkflowExecuted(UserId userId, WorkflowId workflowId);

	/*
	 * -------------------------------------------------------------------------
	 * User reporting
	 * -------------------------------------------------------------------------
	 */

	UserLoginSummary getUserLoginSummary(UUID userId);

	List<UserLoginSummary> getUserLoginSummaries();

	UserActivitySummary getUserActivity(UUID userId);

	List<UserActivitySummary> getUserActivity();

	List<UserAssistantSummary> getUserAssistantUsage(UUID userId);

	/*
	 * -------------------------------------------------------------------------
	 * Assistant reporting
	 * -------------------------------------------------------------------------
	 */

	AssistantPopularity getAssistantPopularity(UUID assistantId);

	List<AssistantPopularity> getAssistantPopularity();

	List<AssistantDailyUsage> getAssistantDailyUsage(LocalDate from, LocalDate to);

	List<AssistantDailyUsage> getAssistantDailyUsage(UUID assistantId, LocalDate from, LocalDate to);

	List<AssistantLeaderboard> getAssistantLeaderboard();

	/*
	 * -------------------------------------------------------------------------
	 * Action reporting
	 * -------------------------------------------------------------------------
	 */

	UserActionSummary getUserActionSummary(UUID userId);

	List<UserActionSummary> getUserActionSummaries();

	List<UserWorkflowSummary> getUserWorkflowSummary(UUID userId);

	List<UserWorkflowSummary> getUserWorkflowSummaryByWorkflow(UUID workflowId);

	List<UserWorkflowSummary> getMostUsedWorkflowsByUser(UUID userId);

	List<UserWorkflowSummary> getMostUsedWorkflows();

	WorkflowPopularity getWorkflowPopularity(UUID workflowId);

	List<WorkflowPopularity> getWorkflowPopularity();

	List<WorkflowPopularity> getMostRunWorkflows();

	/*
	 * -------------------------------------------------------------------------
	 * System reporting
	 * -------------------------------------------------------------------------
	 */

	SystemOverview getSystemOverview();

	List<DailySystemStatistics> getDailySystemStatistics(LocalDate from, LocalDate to);

	/*
	 * ------------------------------------------------------------------------- LLM
	 * reporting
	 * -------------------------------------------------------------------------
	 */

	List<LlmDailyMetrics> getLlmDailyMetrics(LocalDate from, LocalDate to);

	List<LlmHourlyMetrics> getLlmHourlyMetrics(LocalDate from, LocalDate to);

	LlmUserMetrics getLlmUserMetrics(UUID userId);

	List<LlmUserMetrics> getLlmUserMetrics();

	LlmAssistantMetrics getLlmAssistantMetrics(UUID assistantId);

	List<LlmAssistantMetrics> getLlmAssistantMetrics();

	LlmSystemOverview getLlmSystemOverview();

}
