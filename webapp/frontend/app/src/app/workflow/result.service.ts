import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map, shareReplay, switchMap } from 'rxjs/operators';
import { EMPTY, Observable, of, Subscription, throwError, timer } from 'rxjs';
import { TrackableSubject } from '../trackable-subject';
import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';
import { WorkflowState } from '../model/workflow/workflow-state';
import { WorkflowResult } from '../model/workflow/workflow-result';

@Injectable({
	providedIn: 'root'
})
export class ResultService {

	private static mimeToExtension = {
		'text/plain': 'txt',
		'text/html': 'html',
		'application/json': 'json',
	};

	private workflowResultListSubject: TrackableSubject<WorkflowState[]> = new TrackableSubject<WorkflowState[]>();
	workflowResultList$: Observable<WorkflowState[]> = this.workflowResultListSubject.asObservable();

	private static readonly GetWorkflowResultList = 'api/result/list';
	private static readonly GetWorkflowResult = 'api/result';
	private static readonly GetWorkflowLog = 'api/result/log';
	private static readonly DeleteWorkflowResult = 'api/result';

	constructor(private http: HttpClient, private alertService: AlertService) {
		this.startPolling();
	}

	private startPolling() {

		this.workflowResultList$ = timer(0, 5000)
			.pipe(
				switchMap(() => this.getWorkflowResultList().pipe(
					catchError(err => {
						this.alertService.postFailure(JSON.stringify(err));
						return of([] as WorkflowState[]);
					}))
				),
				shareReplay({
					bufferSize: 1,
					refCount: true
				})
			);
	}

	getWorkflowResultList(): Observable<WorkflowState[]> {
		return this.http.get<ApiResult>(ResultService.GetWorkflowResultList)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return throwError(() => new Error(error));
				}),
				map((result: ApiResult) => {
					return this.sortResults(result.data as WorkflowState[]);
				})
			);
	}

	getWorkflowResult(workflowId: string): Observable<WorkflowResult> {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		return this.http.get<ApiResult>(ResultService.GetWorkflowResult, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as WorkflowResult;
				})
			);
	}

	getWorkflowLog(workflowId: string): Observable<string> {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		return this.http.get<ApiResult>(ResultService.GetWorkflowLog, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	openWorkflowOutput(workflowId: string) {
		this.getWorkflowResult(workflowId).subscribe(result => {
			const blob = new Blob([result.output], { type: result.outputFormat });
			const url = URL.createObjectURL(blob);
			window.open(url, '_blank');
			URL.revokeObjectURL(url);
		});
	}

	downloadWorkflowOutput(workflowId: string) {
		this.getWorkflowResult(workflowId).subscribe(result => {
			const blob = new Blob([result.output], { type: result.outputFormat });
			const url = URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = result.name + '.' + ResultService.mimeToExtension[result.outputFormat];
			link.style.display = 'none';
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		});
	}

	downloadWorkflowLog(workflowId: string) {
		this.getWorkflowLog(workflowId).subscribe(result => {
			const blob = new Blob([result], { type: 'text/plain' });
			const url = URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = workflowId + '.txt';
			link.style.display = 'none';
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		});
	}

	deleteWorkflowResult(workflowId: string): Observable<unknown> {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		return this.http.delete<ApiResult>(ResultService.DeleteWorkflowResult, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return null;
				})
			);
	}

	private sortResults(results: WorkflowState[]): WorkflowState[] {
		return results.sort((left, right) => {
			if (!left.name && !right.name) {
				return left.id.localeCompare(right.id);
			}
			if (!left.name) {
				return 1;
			}
			if (!right.name) {
				return -1;
			}
			return left.name.localeCompare(right.name);
		});
	}
}