package tom.analytics.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tom.analytics.entity.AssistantDailyUsage;
import tom.analytics.entity.AssistantLeaderboard;
import tom.analytics.entity.AssistantPopularity;
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
import tom.analytics.service.AnalyticsService;
import tom.controller.ResponseWrapper;

@RestController
@RequestMapping("/api/analytics")

public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	/*
	 * -------------------------------------------------------------------------
	 * User reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/users/{userId}/login")
	public ResponseWrapper<UserLoginSummary> getUserLoginSummary(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserLoginSummary(userId));
	}

	@GetMapping("/users/login")
	public ResponseWrapper<List<UserLoginSummary>> getUserLoginSummaries() {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserLoginSummaries());
	}

	@GetMapping("/users/{userId}/activity")
	public ResponseWrapper<UserActivitySummary> getUserActivity(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserActivity(userId));
	}

	@GetMapping("/users/activity")
	public ResponseWrapper<List<UserActivitySummary>> getUserActivity() {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserActivity());
	}

	@GetMapping("/users/{userId}/assistants")
	public ResponseWrapper<List<UserAssistantSummary>> getUserAssistantUsage(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserAssistantUsage(userId));
	}

	/*
	 * -------------------------------------------------------------------------
	 * User actions
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/users/{userId}/actions")
	public ResponseWrapper<UserActionSummary> getUserActionSummary(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserActionSummary(userId));
	}

	@GetMapping("/users/actions")
	public ResponseWrapper<List<UserActionSummary>> getUserActionSummaries() {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserActionSummaries());
	}

	/*
	 * -------------------------------------------------------------------------
	 * User workflow reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/users/{userId}/workflows")
	public ResponseWrapper<List<UserWorkflowSummary>> getUserWorkflowSummary(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserWorkflowSummary(userId));
	}

	@GetMapping("/users/{userId}/workflows/most-used")
	public ResponseWrapper<List<UserWorkflowSummary>> getMostUsedWorkflowsByUser(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getMostUsedWorkflowsByUser(userId));
	}

	@GetMapping("/workflows/{workflowId}/users")
	public ResponseWrapper<List<UserWorkflowSummary>> getUserWorkflowSummaryByWorkflow(@PathVariable UUID workflowId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getUserWorkflowSummaryByWorkflow(workflowId));
	}

	/*
	 * -------------------------------------------------------------------------
	 * Workflow reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/workflows")
	public ResponseWrapper<List<WorkflowPopularity>> getWorkflowPopularity() {

		return ResponseWrapper.SuccessResponse(analyticsService.getWorkflowPopularity());
	}

	@GetMapping("/workflows/most-run")
	public ResponseWrapper<List<WorkflowPopularity>> getMostRunWorkflows() {

		return ResponseWrapper.SuccessResponse(analyticsService.getMostRunWorkflows());
	}

	@GetMapping("/workflows/{workflowId}")
	public ResponseWrapper<WorkflowPopularity> getWorkflowPopularity(@PathVariable UUID workflowId) {

		WorkflowPopularity result = analyticsService.getWorkflowPopularity(workflowId);

		if (result == null) {
			return ResponseWrapper.FailureResponse(HttpStatus.NOT_FOUND.value(), "Workflow ID not found.");
		}

		return ResponseWrapper.SuccessResponse(result);
	}

	/*
	 * -------------------------------------------------------------------------
	 * Assistant reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/assistants")
	public ResponseWrapper<List<AssistantPopularity>> getAssistantPopularity() {

		return ResponseWrapper.SuccessResponse(analyticsService.getAssistantPopularity());
	}

	@GetMapping("/assistants/{assistantId}")
	public ResponseWrapper<AssistantPopularity> getAssistantPopularity(@PathVariable UUID assistantId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getAssistantPopularity(assistantId));
	}

	@GetMapping("/assistants/leaderboard")
	public ResponseWrapper<List<AssistantLeaderboard>> getAssistantLeaderboard() {

		return ResponseWrapper.SuccessResponse(analyticsService.getAssistantLeaderboard());
	}

	@GetMapping("/assistants/daily")
	public ResponseWrapper<List<AssistantDailyUsage>> getAssistantDailyUsage(@RequestParam LocalDate from,
			@RequestParam LocalDate to) {

		return ResponseWrapper.SuccessResponse(analyticsService.getAssistantDailyUsage(from, to));
	}

	@GetMapping("/assistants/{assistantId}/daily")
	public ResponseWrapper<List<AssistantDailyUsage>> getAssistantDailyUsage(@PathVariable UUID assistantId,
			@RequestParam LocalDate from, @RequestParam LocalDate to) {

		return ResponseWrapper.SuccessResponse(analyticsService.getAssistantDailyUsage(assistantId, from, to));
	}

	/*
	 * -------------------------------------------------------------------------
	 * System reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/system")
	public ResponseWrapper<SystemOverview> getSystemOverview() {

		return ResponseWrapper.SuccessResponse(analyticsService.getSystemOverview());
	}

	@GetMapping("/system/daily")
	public ResponseWrapper<List<DailySystemStatistics>> getDailySystemStatistics(@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		return ResponseWrapper.SuccessResponse(analyticsService.getDailySystemStatistics(from, to));
	}

	/*
	 * ------------------------------------------------------------------------- LLM
	 * reporting
	 * -------------------------------------------------------------------------
	 */

	@GetMapping("/llm/daily")
	public ResponseWrapper<List<LlmDailyMetrics>> getLlmDailyMetrics(@RequestParam LocalDate from,
			@RequestParam LocalDate to) {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmDailyMetrics(from, to));
	}

	@GetMapping("/llm/hourly")
	public ResponseWrapper<List<LlmHourlyMetrics>> getLlmHourlyMetrics(@RequestParam LocalDate from,
			@RequestParam LocalDate to) {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmHourlyMetrics(from, to));
	}

	@GetMapping("/llm/users")
	public ResponseWrapper<List<LlmUserMetrics>> getLlmUserMetrics() {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmUserMetrics());
	}

	@GetMapping("/llm/users/{userId}")
	public ResponseWrapper<LlmUserMetrics> getLlmUserMetrics(@PathVariable UUID userId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmUserMetrics(userId));
	}

	@GetMapping("/llm/assistants")
	public ResponseWrapper<List<LlmAssistantMetrics>> getLlmAssistantMetrics() {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmAssistantMetrics());
	}

	@GetMapping("/llm/assistants/{assistantId}")
	public ResponseWrapper<LlmAssistantMetrics> getLlmAssistantMetrics(@PathVariable UUID assistantId) {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmAssistantMetrics(assistantId));
	}

	@GetMapping("/llm/system")
	public ResponseWrapper<LlmSystemOverview> getLlmSystemOverview() {

		return ResponseWrapper.SuccessResponse(analyticsService.getLlmSystemOverview());
	}
}
