import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { EMPTY, Observable } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';

export interface AnalyticsRow {
	[key: string]: unknown;
}

export interface AnalyticsMetric {
	name: string;
	value: number;
}

export interface AnalyticsDataset {
	name: string;
	rows: AnalyticsRow[];
}

@Injectable({
	providedIn: 'root'
})
export class AnalyticsService {

	private static readonly BaseUrl = 'api/analytics';
	private static readonly GetDailySystemStatistics = 'api/analytics/system/daily';

	constructor(
		private http: HttpClient,
		private alertService: AlertService
	) {
	}

	// -------------------------------------------------------------------------
	// User reporting
	// -------------------------------------------------------------------------

	getUserLoginSummary(userId: string): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/users/${userId}/login`
		);
	}

	getUserLoginSummaries(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/login`
		);
	}

	getUserActivity(userId: string): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/users/${userId}/activity`
		);
	}

	getUserActivitySummaries(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/activity`
		);
	}

	getUserAssistantUsage(userId: string): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/${userId}/assistants`
		);
	}

	// -------------------------------------------------------------------------
	// User actions
	// -------------------------------------------------------------------------

	getUserActionSummary(userId: string): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/users/${userId}/actions`
		);
	}

	getUserActionSummaries(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/actions`
		);
	}

	// -------------------------------------------------------------------------
	// Workflow reporting
	// -------------------------------------------------------------------------

	getUserWorkflows(userId: string): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/${userId}/workflows`
		);
	}

	getMostUsedWorkflowsByUser(userId: string): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/users/${userId}/workflows/most-used`
		);
	}

	getWorkflowUsers(workflowId: string): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/workflows/${workflowId}/users`
		);
	}

	getWorkflowPopularity(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/workflows`
		);
	}

	getMostRunWorkflows(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/workflows/most-run`
		);
	}

	getWorkflowPopularityById(workflowId: string): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/workflows/${workflowId}`
		);
	}

	// -------------------------------------------------------------------------
	// Assistant reporting
	// -------------------------------------------------------------------------

	getAssistantPopularity(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/assistants`
		);
	}

	getAssistantPopularityById(
		assistantId: string
	): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/assistants/${assistantId}`
		);
	}

	getAssistantLeaderboard(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/assistants/leaderboard`
		);
	}

	getAssistantDailyUsage(
		from: Date,
		to: Date
	): Observable<AnalyticsRow[]> {
		return this.getWithDates<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/assistants/daily`,
			from,
			to
		);
	}

	getAssistantDailyUsageForAssistant(
		assistantId: string,
		from: Date,
		to: Date
	): Observable<AnalyticsRow[]> {
		return this.getWithDates<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/assistants/${assistantId}/daily`,
			from,
			to
		);
	}

	// -------------------------------------------------------------------------
	// System reporting
	// -------------------------------------------------------------------------

	getSystemOverview(): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/system`
		);
	}

	getDailySystemStatistics(from: Date, to: Date): Observable<AnalyticsRow[]> {
		const params = {
			from: this.formatDate(from),
			to: this.formatDate(to)
		};

		return this.http
			.get<ApiResult>(
				AnalyticsService.GetDailySystemStatistics,
				{ params }
			)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return (result.data ?? []) as AnalyticsRow[];
				})
			);
	}
	// -------------------------------------------------------------------------
	// LLM reporting
	// -------------------------------------------------------------------------

	getLlmDailyMetrics(
		from: Date,
		to: Date
	): Observable<AnalyticsRow[]> {
		return this.getWithDates<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/llm/daily`,
			from,
			to
		);
	}

	getLlmHourlyMetrics(
		from: Date,
		to: Date
	): Observable<AnalyticsRow[]> {
		return this.getWithDates<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/llm/hourly`,
			from,
			to
		);
	}

	getLlmUserMetrics(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/llm/users`
		);
	}

	getLlmUserMetricsById(userId: string): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/llm/users/${userId}`
		);
	}

	getLlmAssistantMetrics(): Observable<AnalyticsRow[]> {
		return this.get<AnalyticsRow[]>(
			`${AnalyticsService.BaseUrl}/llm/assistants`
		);
	}

	getLlmAssistantMetricsById(
		assistantId: string
	): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/llm/assistants/${assistantId}`
		);
	}

	getLlmSystemOverview(): Observable<AnalyticsRow> {
		return this.get<AnalyticsRow>(
			`${AnalyticsService.BaseUrl}/llm/system`
		);
	}

	// -------------------------------------------------------------------------
	// HTTP helpers
	// -------------------------------------------------------------------------

	private get<T>(url: string): Observable<T> {
		return this.http.get<ApiResult>(url)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map(result => result.data as T)
			);
	}

	private getWithDates<T>(
		url: string,
		from: Date,
		to: Date
	): Observable<T> {

		const params = new HttpParams()
			.set('from', this.formatDate(from))
			.set('to', this.formatDate(to));

		return this.http.get<ApiResult>(url, { params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map(result => result.data as T)
			);
	}

	private formatDate(date: Date): string {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		const day = String(date.getDate()).padStart(2, '0');

		return `${year}-${month}-${day}`;
	}
}