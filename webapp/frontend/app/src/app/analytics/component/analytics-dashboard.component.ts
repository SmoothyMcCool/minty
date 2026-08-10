import { CommonModule, NgTemplateOutlet } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';

import { AnalyticsRow, AnalyticsService } from '../analytics.service';

interface ChartPoint {
	label: string;
	value: number;
	percentage: number;
}

@Component({
	selector: 'minty-analytics-dashboard',
	standalone: true,
	imports: [CommonModule, NgTemplateOutlet],
	templateUrl: './analytics-dashboard.component.html',
	styleUrl: './analytics-dashboard.component.css'
})
export class AnalyticsDashboardComponent implements OnInit {

	fromDate = this.getDateDaysAgo(30);
	toDate = new Date();

	loading = false;
	loadingDateRange = false;

	systemOverview: AnalyticsRow | null = null;
	dailySystemStatistics: AnalyticsRow[] = [];
	llmSystemOverview: AnalyticsRow | null = null;

	assistantLeaderboard: AnalyticsRow[] = [];
	assistantPopularity: AnalyticsRow[] = [];
	assistantDailyUsage: AnalyticsRow[] = [];

	workflowPopularity: AnalyticsRow[] = [];
	mostRunWorkflows: AnalyticsRow[] = [];

	userActivity: AnalyticsRow[] = [];
	userLoginSummaries: AnalyticsRow[] = [];
	userActionSummaries: AnalyticsRow[] = [];

	llmDailyMetrics: AnalyticsRow[] = [];
	llmHourlyMetrics: AnalyticsRow[] = [];
	llmUserMetrics: AnalyticsRow[] = [];
	llmAssistantMetrics: AnalyticsRow[] = [];

	constructor(
		private analyticsService: AnalyticsService
	) {
	}

	ngOnInit(): void {
		this.loadDashboard();
	}

	loadDashboard(): void {
		this.loading = true;

		forkJoin({
			systemOverview: this.analyticsService.getSystemOverview(),
			llmSystemOverview: this.analyticsService.getLlmSystemOverview(),

			assistantLeaderboard:
				this.analyticsService.getAssistantLeaderboard(),

			assistantPopularity:
				this.analyticsService.getAssistantPopularity(),

			workflowPopularity:
				this.analyticsService.getWorkflowPopularity(),

			mostRunWorkflows:
				this.analyticsService.getMostRunWorkflows(),

			userActivity:
				this.analyticsService.getUserActivitySummaries(),

			userLoginSummaries:
				this.analyticsService.getUserLoginSummaries(),

			userActionSummaries:
				this.analyticsService.getUserActionSummaries(),

			llmUserMetrics:
				this.analyticsService.getLlmUserMetrics(),

			llmAssistantMetrics:
				this.analyticsService.getLlmAssistantMetrics()
		}).subscribe({
			next: result => {
				this.systemOverview = result.systemOverview;
				this.llmSystemOverview = result.llmSystemOverview;

				this.assistantLeaderboard =
					result.assistantLeaderboard ?? [];

				this.assistantPopularity =
					result.assistantPopularity ?? [];

				this.workflowPopularity =
					result.workflowPopularity ?? [];

				this.mostRunWorkflows =
					result.mostRunWorkflows ?? [];

				this.userActivity =
					result.userActivity ?? [];

				this.userLoginSummaries =
					result.userLoginSummaries ?? [];

				this.userActionSummaries =
					result.userActionSummaries ?? [];

				this.llmUserMetrics =
					result.llmUserMetrics ?? [];

				this.llmAssistantMetrics =
					result.llmAssistantMetrics ?? [];

				this.loading = false;

				this.loadDateRangeData();
			},
			error: () => {
				this.loading = false;
			}
		});
	}

	loadDateRangeData(): void {
		this.loadingDateRange = true;

		forkJoin({
			dailySystemStatistics:
				this.analyticsService.getDailySystemStatistics(
					this.fromDate,
					this.toDate
				),

			assistantDailyUsage:
				this.analyticsService.getAssistantDailyUsage(
					this.fromDate,
					this.toDate
				),

			llmDailyMetrics:
				this.analyticsService.getLlmDailyMetrics(
					this.fromDate,
					this.toDate
				),

			llmHourlyMetrics:
				this.analyticsService.getLlmHourlyMetrics(
					this.fromDate,
					this.toDate
				)
		}).subscribe({
			next: result => {
				this.dailySystemStatistics =
					this.sortDailyDescending(
						result.dailySystemStatistics ?? []
					);

				this.assistantDailyUsage =
					this.sortDailyDescending(
						result.assistantDailyUsage ?? []
					);

				this.llmDailyMetrics =
					this.sortDailyDescending(
						result.llmDailyMetrics ?? []
					);

				this.llmHourlyMetrics =
					this.sortHourlyDescending(
						result.llmHourlyMetrics ?? []
					);

				this.loadingDateRange = false;
			},
			error: () => {
				this.loadingDateRange = false;
			}
		});
	}

	refresh(): void {
		this.loadDashboard();
	}

	dateRangeChanged(): void {
		if (this.fromDate > this.toDate) {
			return;
		}

		this.loadDateRangeData();
	}

	setFromDate(value: string): void {
		this.fromDate = this.parseInputDate(value);
		this.dateRangeChanged();
	}

	setToDate(value: string): void {
		this.toDate = this.parseInputDate(value);
		this.dateRangeChanged();
	}

	formatDateForInput(date: Date): string {
		const year = date.getFullYear();
		const month = String(date.getMonth() + 1).padStart(2, '0');
		const day = String(date.getDate()).padStart(2, '0');

		return `${year}-${month}-${day}`;
	}

	getOverviewMetrics(
		overview: AnalyticsRow | null
	): AnalyticsMetric[] {
		if (!overview) {
			return [];
		}

		return Object.entries(overview)
			.filter(([name, value]) =>
				!this.isIdColumn(name) &&
				this.isNumeric(value)
			)
			.map(([name, value]) => ({
				name,
				value: Number(value)
			}));
	}

	getColumns(rows: AnalyticsRow[]): string[] {
		if (!rows || rows.length === 0) {
			return [];
		}

		const columns = new Set<string>();

		for (const row of rows) {
			for (const key of Object.keys(row)) {
				/*
				 * IDs are useful internally but are not useful in the
				 * dashboard tables. Hide anything whose property name
				 * ends in "Id" or "ID".
				 *
				 * This covers:
				 *   id
				 *   userId
				 *   assistantId
				 *   workflowId
				 *   conversationId
				 *   etc.
				 */
				if (this.isIdColumn(key)) {
					continue;
				}

				columns.add(key);
			}
		}

		return Array.from(columns);
	}

	getNumericColumns(rows: AnalyticsRow[]): string[] {
		return this.getColumns(rows)
			.filter(column =>
				rows.some(row =>
					this.isNumeric(row[column])
				)
			);
	}

	getChartPoints(
		rows: AnalyticsRow[],
		valueColumn?: string
	): ChartPoint[] {

		if (!rows || rows.length === 0) {
			return [];
		}

		const numericColumns =
			this.getNumericColumns(rows);

		const selectedColumn =
			valueColumn ??
			this.findPreferredNumericColumn(
				numericColumns
			);

		if (!selectedColumn) {
			return [];
		}

		const values = rows.map(row => {
			const value = Number(row[selectedColumn]);

			return Number.isFinite(value)
				? value
				: 0;
		});

		const maximum =
			Math.max(...values, 1);

		return rows.map((row, index) => ({
			label: this.findLabel(row, index),
			value: values[index],
			percentage:
				(values[index] / maximum) * 100
		}));
	}

	getDefaultChartColumn(
		rows: AnalyticsRow[]
	): string | null {

		const columns =
			this.getNumericColumns(rows);

		return this.findPreferredNumericColumn(
			columns
		);
	}

	getTotal(
		rows: AnalyticsRow[],
		column: string | null
	): number {

		if (!column) {
			return 0;
		}

		return rows.reduce(
			(total, row) => {
				const value = Number(row[column]);

				return total +
					(Number.isFinite(value) ? value : 0);
			},
			0
		);
	}

	formatColumnName(column: string): string {
		return column
			.replace(/([a-z])([A-Z])/g, '$1 $2')
			.replace(/_/g, ' ')
			.replace(/\b\w/g, value =>
				value.toUpperCase()
			);
	}

	formatValue(value: unknown): string {
		if (value === null || value === undefined) {
			return '';
		}

		if (typeof value === 'boolean') {
			return value ? 'Yes' : 'No';
		}

		if (typeof value === 'number') {
			return Number.isInteger(value)
				? value.toLocaleString()
				: value.toLocaleString(
					undefined,
					{
						maximumFractionDigits: 2
					}
				);
		}

		if (value instanceof Date) {
			return value.toLocaleString();
		}

		if (typeof value === 'object') {
			return JSON.stringify(value);
		}

		return String(value);
	}

	getValue(
		row: AnalyticsRow,
		column: string
	): unknown {
		return row[column];
	}

	findLabel(
		row: AnalyticsRow,
		index: number
	): string {

		/*
		 * Deliberately do not use ID fields here. Even though ID
		 * columns are hidden from tables, using an ID as a chart
		 * label would still expose it visually.
		 */
		const preferredNames = [
			'name',
			'day',
			'date',
			'assistantName',
			'workflowName',
			'userName',
			'hour'
		];

		for (const name of preferredNames) {
			if (
				row[name] !== null &&
				row[name] !== undefined
			) {
				return String(row[name]);
			}
		}

		return String(index + 1);
	}

	private isNumeric(value: unknown): boolean {
		if (typeof value === 'number') {
			return Number.isFinite(value);
		}

		if (typeof value === 'string' && value.trim() !== '') {
			return Number.isFinite(Number(value));
		}

		return false;
	}

	private isIdColumn(column: string): boolean {
		return column.toLowerCase() === 'id' ||
			column.toLowerCase().endsWith('id');
	}

	private sortDailyDescending(
		rows: AnalyticsRow[]
	): AnalyticsRow[] {

		return [...rows].sort((a, b) => {
			const aDay = this.getComparableDay(a);
			const bDay = this.getComparableDay(b);

			return bDay.localeCompare(aDay);
		});
	}

	private sortHourlyDescending(
		rows: AnalyticsRow[]
	): AnalyticsRow[] {

		return [...rows].sort((a, b) => {
			const aDay = this.getComparableDay(a);
			const bDay = this.getComparableDay(b);

			const dayComparison =
				bDay.localeCompare(aDay);

			if (dayComparison !== 0) {
				return dayComparison;
			}

			const aHour = this.getHour(a);
			const bHour = this.getHour(b);

			return bHour - aHour;
		});
	}

	private getComparableDay(
		row: AnalyticsRow
	): string {

		const value =
			row['day'] ??
			row['date'];

		if (value === null || value === undefined) {
			return '';
		}

		if (value instanceof Date) {
			return this.formatDateForInput(value);
		}

		return String(value);
	}

	private getHour(
		row: AnalyticsRow
	): number {

		const value = row['hour'];

		if (typeof value === 'number') {
			return value;
		}

		if (
			typeof value === 'string' &&
			value.trim() !== ''
		) {
			const parsed = Number(value);

			return Number.isFinite(parsed)
				? parsed
				: -1;
		}

		return -1;
	}

	private findPreferredNumericColumn(
		columns: string[]
	): string | null {

		if (columns.length === 0) {
			return null;
		}

		const preferredNames = [
			'count',
			'conversationCount',
			'messageCount',
			'executionCount',
			'requests',
			'totalRequests',
			'tokens',
			'totalTokens',
			'usage',
			'value'
		];

		for (const preferred of preferredNames) {
			const match = columns.find(
				column =>
					column.toLowerCase() ===
					preferred.toLowerCase()
			);

			if (match) {
				return match;
			}
		}

		return columns[0];
	}

	private getDateDaysAgo(days: number): Date {
		const date = new Date();

		date.setDate(
			date.getDate() - days
		);

		return date;
	}

	private parseInputDate(value: string): Date {
		const parts = value.split('-');

		return new Date(
			Number(parts[0]),
			Number(parts[1]) - 1,
			Number(parts[2])
		);
	}
}

interface AnalyticsMetric {
	name: string;
	value: number;
}

interface ChartPoint {
	label: string;
	value: number;
	percentage: number;
}
