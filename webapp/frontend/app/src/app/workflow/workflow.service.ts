import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable, Subject, throwError, timer } from "rxjs";
import { WorkflowTask } from "../model/workflow-task";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";

@Injectable({
    providedIn: 'root'
})
export class WorkflowService{

    private resultListSubject: Subject<string[]> = new Subject<string[]>();
    resultList$: Observable<string[]> = this.resultListSubject.asObservable();

    private static readonly ExecuteTask = 'api/workflow/execute';
    private static readonly GetTask = 'api/workflow/task';
    private static readonly DeleteTask = 'api/workflow/task';
    private static readonly GetTaskConfig = 'api/workflow/config';
    private static readonly ListWorkflowTasks = 'api/workflow/task/list';
    private static readonly ListTriggeredWorkflowTasks = 'api/workflow/task/trigger/list';
    private static readonly NewTask = 'api/workflow/new';
    private static readonly NewTriggeredTask = 'api/workflow/trigger/new';
    private static readonly ListWorkflows = 'api/workflow/workflows';
    private static readonly GetResultList = 'api/workflow/resultList';
    private static readonly GetResult  = 'api/workflow/result';
    private static readonly DeleteResult  = 'api/workflow/result';

    constructor(private http: HttpClient, private alertService: AlertService) {
        this.doRefresh();
    }

    private refreshResultList() {
        timer(5000)
            .subscribe(() => {
                this.doRefresh();
            });
    }

    private doRefresh() {
        this.getResultList()
            .pipe(
                catchError(() => {
                    this.refreshResultList();
                    return EMPTY;
                }),
                map((result: string[]) => {
                    this.resultListSubject.next(result);
                    this.refreshResultList();
                })
            ).subscribe();
    }

    getResultList(): Observable<string[]> {
        return this.http.get<ApiResult>(WorkflowService.GetResultList)
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

    getResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultId', resultId);
        
        return this.http.get<ApiResult>(WorkflowService.GetResult, { params: params })
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

    deleteResult(resultId: string): Observable<unknown> {
        let params: HttpParams = new HttpParams();
        params = params.append('resultName', resultId);

        return this.http.delete<ApiResult>(WorkflowService.DeleteResult, { params: params })
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

    getTask(taskId: number): Observable<WorkflowTask> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(WorkflowService.GetTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const ret = result.data as WorkflowTask;
                    ret.defaultConfig = new Map(Object.entries(ret.defaultConfig));
                    return ret;
                })
            );
    }

    deleteTask(task: WorkflowTask): Observable<WorkflowTask[]> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', task.id);

        return this.http.delete<ApiResult>(WorkflowService.DeleteTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as WorkflowTask[];
                })
            );
    }

    getTaskConfig(taskId: number): Observable<Map<string, string>> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(WorkflowService.GetTaskConfig, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return new Map<string, string>(Object.entries(result.data as any));
                })
            );
    }

    listWorkflowTasks(): Observable<WorkflowTask[]> {
        return this.http.get<ApiResult>(WorkflowService.ListWorkflowTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as WorkflowTask[];
                })
            );
    }

    listTriggeredWorkflowTasks(): Observable<WorkflowTask[]> {
        return this.http.get<ApiResult>(WorkflowService.ListTriggeredWorkflowTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as WorkflowTask[];
                })
            );
    }

    listWorkflows(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(WorkflowService.ListWorkflows)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const map = new Map<string, Map<string, string>>(Object.entries(result.data as any));
                    const resultMap = new Map<string, Map<string, string>>();

                    map.forEach((value, key, map) => {
                        resultMap.set(key, new Map<string, string>(Object.entries(value)));
                    });

                    return resultMap;
                })
            );
    }

    newTask(task: WorkflowTask): Observable<WorkflowTask> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.defaultConfig)
        const transmitTask = task as any;
        transmitTask.defaultConfig = configObj;

        return this.http.post<ApiResult>(WorkflowService.NewTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as WorkflowTask;
                })
            );
    }

    newTriggeredTask(task: WorkflowTask, directory: string): Observable<WorkflowTask> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.defaultConfig)
        const transmitTask = task as any;
        transmitTask.defaultConfig = configObj;
        transmitTask.directory = directory;

        return this.http.post<ApiResult>(WorkflowService.NewTriggeredTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as WorkflowTask;
                })
            );
    }

    execute(request: string, data: Map<String, String>): Observable<string> {

        const body = {
            request: request,
            data: Object.fromEntries(data)
        };
        return this.http.post<ApiResult>(WorkflowService.ExecuteTask, body)
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
}