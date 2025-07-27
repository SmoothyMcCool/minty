import { Injectable } from "@angular/core";
import { Workflow } from "../model/workflow";
import { HttpClient, HttpParams } from "@angular/common/http";
import { AlertService } from "../alert.service";
import { catchError, EMPTY, map, Observable } from "rxjs";
import { ApiResult } from "../model/api-result";

@Injectable({
    providedIn: 'root'
})
export class WorkflowService {

    private static readonly GetWorkflow = 'api/workflow'; // Retrieve a workflow by ID
    private static readonly DeleteWorkflow = 'api/workflow'; // Delete a workflow
    private static readonly ListWorkflows = 'api/workflow/list'; // Retrieve all workflows
    private static readonly ExecuteWorkflow = 'api/workflow/execute'; // Run a workflow
    private static readonly NewWorkflow = 'api/workflow/new'; // Create a new workflow

    constructor(private http: HttpClient, private alertService: AlertService) {
    }

    getWorkflow(workflowId: number): Observable<Workflow> {
        let params: HttpParams = new HttpParams();
        params = params.append('workflowId', workflowId);

        return this.http.get<ApiResult>(WorkflowService.GetWorkflow, { params: params })
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

    deleteWorkflow(workflowId: number): Observable<Workflow[]> {
        let params: HttpParams = new HttpParams();
        params = params.append('workflowId', workflowId);

        return this.http.delete<ApiResult>(WorkflowService.DeleteWorkflow, { params: params })
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

    newWorkflow(workflow: Workflow): Observable<string> {
        const body = {
            id: workflow.id,
            name: workflow.name,
            description: workflow.description,
            shared: workflow.shared,
            workflowSteps: workflow.workflowSteps.map(item => {
                return {
                    name: item.name,
                    configuration: Object.fromEntries(item.configuration)
                }
            }),
            outputStep: {
                name: workflow.outputStep.name,
                configuration: Object.fromEntries(workflow.outputStep.configuration)
            }
        };

        return this.http.post<ApiResult>(WorkflowService.NewWorkflow, body)
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

    listWorkflows(): Observable<Workflow[]> {
        return this.http.get<ApiResult>(WorkflowService.ListWorkflows)
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

    execute(workflow: Workflow): Observable<string> {
        const body = {
            id: workflow.id,
            taskConfigurationList: workflow.workflowSteps.map(step => Object.fromEntries(step.configuration)),
            outputConfiguration: Object.fromEntries(workflow.outputStep.configuration)
        }

        return this.http.post<ApiResult>(WorkflowService.ExecuteWorkflow, body)
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

        private makeProper(workflow: any): Workflow {
            let w: Workflow = {
                id: workflow.id,
                ownerId: workflow.ownerId,
                name: workflow.name,
                description: workflow.description,
                shared: workflow.shared,
                workflowSteps: (workflow.workflowSteps as any[]).map(element => {
                    return {
                        name: element.name,
                        configuration: new Map(Object.entries(element.configuration))
                    };
                }),
                outputStep: {
                    name: workflow.outputStep.name,
                    configuration: new Map(Object.entries(workflow.outputStep.configuration))
                }
            }
            return w;
        }
}