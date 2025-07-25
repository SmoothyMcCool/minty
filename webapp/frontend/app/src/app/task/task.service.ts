import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { Task } from "../model/task";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";
import { StandaloneTask } from "../model/standalone-task";

interface StandaloneTaskRequest {
    taskRequest: {
        name: string;
        configuration: any
    },
    outputTaskRequest: {
        name: string;
        configuration: any
    }
};

@Injectable({
    providedIn: 'root'
})
export class TaskService {

    private static readonly ExecuteTask = 'api/task/execute'; // Run a created task

    private static readonly NewTask = 'api/task/new'; // Create a new task from a template
    private static readonly GetTask = 'api/task'; // Get a task by ID
    private static readonly DeleteTask = 'api/task'; // Delete a task by ID
    private static readonly ListTasks = 'api/task/list'; // List all executable tasks

    private static readonly NewTriggeredTask = 'api/task/triggered/new'; // Create a new triggered task from a template
    private static readonly ListTriggeredTasks = 'api/task/triggered/list'; // List all registered triggered tasks

    constructor(private http: HttpClient, private alertService: AlertService) {
    }

    execute(task: StandaloneTask): Observable<string> {

        const body: StandaloneTaskRequest = {
            taskRequest: {
                name: task.taskTemplate.name,
                configuration: Object.fromEntries(task.taskTemplate.configuration)
            },
            outputTaskRequest: {
                name: task.outputTemplate.name,
                configuration: Object.fromEntries(task.outputTemplate.configuration)
            }
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

    newTask(task: StandaloneTask): Observable<StandaloneTask> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.taskTemplate.configuration);
        const outputConfigObj = Object.fromEntries(task.outputTemplate.configuration);
        const transmitTask = task as any;
        transmitTask.taskTemplate.configuration = configObj;
        transmitTask.outputTemplate.configuration = outputConfigObj;

        return this.http.post<ApiResult>(TaskService.NewTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return this.makeProper(result.data);
                })
            );
    }

    newTriggeredTask(task: StandaloneTask): Observable<StandaloneTask> {
        // This is necessary because Map objects don't serialize properly.
        const configObj = Object.fromEntries(task.taskTemplate.configuration);
        const outputConfigObj = Object.fromEntries(task.outputTemplate.configuration);
        const transmitTask = task as any;
        transmitTask.taskTemplate.configuration = configObj;
        transmitTask.outputTemplate.configuration = outputConfigObj;
        transmitTask.triggered = true;

        return this.http.post<ApiResult>(TaskService.NewTriggeredTask, transmitTask)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return this.makeProper(result.data);
                })
            );
    }

    getTask(taskId: number): Observable<StandaloneTask> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskService.GetTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return this.makeProper(result.data);
                })
            );
    }

    deleteTask(task: StandaloneTask): Observable<StandaloneTask[]> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', task.id);

        return this.http.delete<ApiResult>(TaskService.DeleteTask, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return Array.from(result.data as any[]).map(element => this.makeProper(element));
                })
            );
    }

    listTasks(): Observable<StandaloneTask[]> {
        return this.http.get<ApiResult>(TaskService.ListTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return Array.from(result.data as any[]).map(element => this.makeProper(element));
                })
            );
    }

    listTriggeredTasks(): Observable<StandaloneTask[]> {
        return this.http.get<ApiResult>(TaskService.ListTriggeredTasks)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return Array.from(result.data as any[]).map(element => this.makeProper(element));
                })
            );
    }

    private makeProper(task: any): StandaloneTask {
        let st: StandaloneTask = {
            id: task.id,
            name: task.name,
            triggered: task.triggered,
            taskTemplate: {
                name: task.taskTemplate.name,
                configuration: new Map(Object.entries(task.taskTemplate.configuration))
            },
            outputTemplate: {
                name: task.outputTemplate.name,
                configuration: new Map(Object.entries(task.outputTemplate.configuration))
            },
        }
        return st;
    }
}