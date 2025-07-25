import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable, throwError, timer } from "rxjs";
import { TrackableSubject } from "./trackable-subject";
import { AlertService } from "./alert.service";
import { ApiResult } from "./model/api-result";

@Injectable({
    providedIn: 'root'
})
export class ResultService {

    private taskResultListSubject: TrackableSubject<string[]> = new TrackableSubject<string[]>();
    taskResultList$: Observable<string[]> = this.taskResultListSubject.asObservable();

    private workflowResultListSubject: TrackableSubject<string[]> = new TrackableSubject<string[]>();
    workflowResultList$: Observable<string[]> = this.workflowResultListSubject.asObservable();

    private static readonly GetTaskResultList = 'api/result/task/list';
    private static readonly GetTaskResult  = 'api/result/task';
    private static readonly DeleteTaskResult  = 'api/result/task';

    private static readonly GetWorkflowResultList = 'api/result/workflow/list';
    private static readonly GetWorkflowResult  = 'api/result/workflow';
    private static readonly DeleteWorkflowResult  = 'api/result/workflow';

    constructor(private http: HttpClient, private alertService: AlertService) {
        this.doTaskRefresh();
        this.doWorkflowRefresh();
    }

    private refreshTaskResultList() {
        timer(5000)
            .subscribe(() => {
                this.doTaskRefresh();
            });
    }

    private doTaskRefresh() {
        if (!this.taskResultListSubject.hasSubscribers()) {
            this.refreshTaskResultList();
            return;
        }

        this.getTaskResultList()
            .pipe(
                catchError(() => {
                    this.refreshTaskResultList();
                    return EMPTY;
                }),
                map((result: string[]) => {
                    this.taskResultListSubject.next(result);
                    this.refreshTaskResultList();
                })
            ).subscribe();
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
                map((result: string[]) => {
                    this.workflowResultListSubject.next(result);
                    this.refreshWorkflowResultList();
                })
            ).subscribe();
    }

    getTaskResultList(): Observable<string[]> {
        return this.http.get<ApiResult>(ResultService.GetTaskResultList)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return throwError(() => new Error(error))
                }),
                map((result: ApiResult) => {
                    return result.data as string[];
                })
            );
    }

    getTaskResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultId', resultId);
        
        return this.http.get<ApiResult>(ResultService.GetTaskResult, { params: params })
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

    deleteTaskResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultName', resultId);

        return this.http.delete<ApiResult>(ResultService.DeleteTaskResult, { params: params })
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

    getWorkflowResultList(): Observable<string[]> {
        return this.http.get<ApiResult>(ResultService.GetWorkflowResultList)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return throwError(() => new Error(error))
                }),
                map((result: ApiResult) => {
                    return result.data as string[];
                })
            );
    }

    getWorkflowResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultId', resultId);
        
        return this.http.get<ApiResult>(ResultService.GetWorkflowResult, { params: params })
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

    deleteWorkflowResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultName', resultId);

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