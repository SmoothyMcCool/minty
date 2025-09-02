import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable, throwError, timer } from 'rxjs';
import { TrackableSubject } from '../trackable-subject';
import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';
import { WorkflowState } from '../model/workflow/workflow-state';
import { WorkflowResult } from '../model/workflow/workflow-result';

@Injectable({
	providedIn: 'root'
})
export class ResultService {

	private workflowResultListSubject: TrackableSubject<WorkflowState[]> = new TrackableSubject<WorkflowState[]>();
	workflowResultList$: Observable<WorkflowState[]> = this.workflowResultListSubject.asObservable();

	private static readonly GetWorkflowResultList = 'api/result/list';
	private static readonly GetWorkflowResult  = 'api/result';
	private static readonly GetWorkflowOutput = 'api/result/output';
	private static readonly DeleteWorkflowResult  = 'api/result';

	constructor(private http: HttpClient, private alertService: AlertService) {
		this.doWorkflowRefresh();
	}

	private refreshWorkflowResultList() {
		timer(5000)
			.subscribe(() => {
				this.doWorkflowRefresh();
			});
	}

	private doWorkflowRefresh() {
		if (!this.workflowResultListSubject.hasSubscribers()) {
			this.refreshWorkflowResultList();
			return;
		}

		this.getWorkflowResultList()
			.pipe(
				catchError(() => {
					this.refreshWorkflowResultList();
					return EMPTY;
				}),
				map((result: WorkflowState[]) => {
					this.workflowResultListSubject.next(result);
					this.refreshWorkflowResultList();
				})
			).subscribe();
	}

	getWorkflowResultList(): Observable<WorkflowState[]> {
		return this.http.get<ApiResult>(ResultService.GetWorkflowResultList)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return throwError(() => new Error(error));
				}),
				map((result: ApiResult) => {
					return result.data as WorkflowState[];
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

	openWorkflowOutput(workflowId: string) {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		this.http.get<ApiResult>(ResultService.GetWorkflowOutput, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			).subscribe((result: string) => {
				const blob = new Blob([result], { type: 'text/html' });
				const url = URL.createObjectURL(blob);
				window.open(url, '_blank');
				URL.revokeObjectURL(url); 
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

}