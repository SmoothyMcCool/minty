import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable, Subject, throwError, timer } from "rxjs";
import { Task } from "../model/task";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";

@Injectable({
    providedIn: 'root'
})
export class TaskService {

    private resultListSubject: Subject<string[]> = new Subject<string[]>();
    resultList$: Observable<string[]> = this.resultListSubject.asObservable();

    private static readonly ExecuteTask = 'api/task/execute'; // Run a created task
    private static readonly NewTask = 'api/task/new'; // Create a new task from a template
    private static readonly NewTriggeredTask = 'api/task/triggered/new'; // Create a new triggered task from a template
    private static readonly GetTask = 'api/task'; // Get a task by ID
    private static readonly DeleteTask = 'api/task'; // Delete a task by ID
    private static readonly ListTasks = 'api/task/list'; // List all executable tasks
    private static readonly ListTriggeredTasks = 'api/task/triggered/list'; // List all registered triggered tasks

    private static readonly ListTemplates = 'api/task/templates'; // List all templates that can be used to create tasks
    private static readonly GetTaskConfig = 'api/task/templates/config'; // Get default configuration of a task
    private static readonly ListOutputTasks = 'api/task/output'; // List all output tasks that can be used to format task output
    private static readonly GetOutputConfig = 'api/task/output/config'; // Get default configuration of an output task

    private static readonly GetResultList = 'api/task/resultList';
    private static readonly GetResult  = 'api/task/result';
    private static readonly DeleteResult  = 'api/task/result';

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

    execute(request: string, data: Map<String, String>, outputTask: string, outputTaskConfig: Map<String, String>): Observable<string> {

        const body = {
            request: request,
            data: Object.fromEntries(data),
            outputTask: outputTask,
            outputTaskConfig: Object.fromEntries(outputTaskConfig)
        };
        return this.http.post<ApiResult>(TaskService.ExecuteTask, body)
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

    newTask(task: Task): Observable<Task> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.defaultConfig)
        const outputConfigObj = Object.fromEntries(task.outputTaskConfig)
        const transmitTask = task as any;
        transmitTask.defaultConfig = configObj;
        transmitTask.outputTaskConfig = outputConfigObj;

        return this.http.post<ApiResult>(TaskService.NewTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Task;
                })
            );
    }

    newTriggeredTask(task: Task, directory: string): Observable<Task> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.defaultConfig)
        const transmitTask = task as any;
        transmitTask.defaultConfig = configObj;
        transmitTask.directory = directory;

        return this.http.post<ApiResult>(TaskService.NewTriggeredTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Task;
                })
            );
    }

    getTask(taskId: number): Observable<Task> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskService.GetTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const ret = result.data as Task;
                    ret.defaultConfig = new Map(Object.entries(ret.defaultConfig));
                    ret.outputTaskConfig = new Map(Object.entries(ret.outputTaskConfig));
                    return ret;
                })
            );
    }

    deleteTask(task: Task): Observable<Task[]> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', task.id);

        return this.http.delete<ApiResult>(TaskService.DeleteTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Task[];
                })
            );
    }

    getTaskConfig(taskId: number): Observable<Map<string, string>> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskService.GetTaskConfig, { params: params })
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

    getOutputConfig(taskId: number): Observable<Map<string, string>> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskService.GetOutputConfig, { params: params })
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

    listTasks(): Observable<Task[]> {
        return this.http.get<ApiResult>(TaskService.ListTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Task[];
                })
            );
    }

    listTriggeredTasks(): Observable<Task[]> {
        return this.http.get<ApiResult>(TaskService.ListTriggeredTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Task[];
                })
            );
    }

    listTemplates(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(TaskService.ListTemplates)
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

    listOutputTasks(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(TaskService.ListOutputTasks)
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

    getResultList(): Observable<string[]> {
        return this.http.get<ApiResult>(TaskService.GetResultList)
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
        
        return this.http.get<ApiResult>(TaskService.GetResult, { params: params })
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

        return this.http.delete<ApiResult>(TaskService.DeleteResult, { params: params })
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