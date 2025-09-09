import { Injectable } from '@angular/core';
import { Workflow } from '../model/workflow/workflow';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../alert.service';
import { catchError, EMPTY, map, Observable } from 'rxjs';
import { ApiResult } from '../model/api-result';
import { TaskDescription } from '../model/task-description';
import { ResultTemplate } from '../model/workflow/result-template';

@Injectable({
	providedIn: 'root'
})
export class WorkflowService {

	private static readonly GetWorkflow = 'api/workflow'; // Retrieve a workflow by ID
	private static readonly DeleteWorkflow = 'api/workflow'; // Delete a workflow
	private static readonly ListWorkflows = 'api/workflow/list'; // Retrieve all workflows
	private static readonly ExecuteWorkflow = 'api/workflow/execute'; // Run a workflow
	private static readonly NewWorkflow = 'api/workflow/new'; // Create a new workflow
	private static readonly UpdateWorkflow = 'api/workflow/update'; // Update an existing workflow
	private static readonly UploadTemplate = 'api/workflow/resultTemplate'; // Upload a new result template
	private static readonly ListTemplates = 'api/workflow/resultTemplate'; // List all result templates

	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	getWorkflow(workflowId: string): Observable<Workflow> {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		return this.http.get<ApiResult>(WorkflowService.GetWorkflow, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return this.objectify(result.data);
				})
			);
	}

	deleteWorkflow(workflowId: string): Observable<Workflow[]> {
		let params: HttpParams = new HttpParams();
		params = params.append('workflowId', workflowId);

		return this.http.delete<ApiResult>(WorkflowService.DeleteWorkflow, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return Array.from(result.data as any[]).map(element => this.objectify(element));
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
				};
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

	updateWorkflow(workflow: Workflow): Observable<Workflow> {
		const body = {
			id: workflow.id,
			name: workflow.name,
			description: workflow.description,
			shared: workflow.shared,
			workflowSteps: workflow.workflowSteps.map(item => {
				return {
					name: item.name,
					configuration: Object.fromEntries(item.configuration)
				};
			}),
			outputStep: {
				name: workflow.outputStep.name,
				configuration: Object.fromEntries(workflow.outputStep.configuration)
			}
		};

		return this.http.post<ApiResult>(WorkflowService.UpdateWorkflow, body)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Workflow;
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
					return Array.from(result.data as any[]).map(element => this.objectify(element));
				})
			);
	}

	execute(workflow: Workflow): Observable<string> {
		const body = {
			id: workflow.id,
			taskConfigurationList: workflow.workflowSteps.map(step => Object.fromEntries(step.configuration)),
			outputConfiguration: Object.fromEntries(workflow.outputStep.configuration)
		};

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

	addResultTemplate(outputTemplate: ResultTemplate): Observable<string> {
		const formData = new FormData();
		formData.append('templateName', outputTemplate.name);
		formData.append('file', outputTemplate.file);

		return this.http.post<ApiResult>(WorkflowService.UploadTemplate, formData)
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

	listResultTemplates(): Observable<string[]> {
		return this.http.get<ApiResult>(WorkflowService.ListTemplates)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string[];
				})
			);
	}

	sanitize(workflow: Workflow, taskTemplates: TaskDescription[], outputTaskTemplates: TaskDescription[], defaults: Map<string, string>) {
		workflow.workflowSteps.forEach(step => {
			const template = taskTemplates.find(template => template.name === step.name);
			if (template) {
				// This removes any keys that should not be present (which happens sometimes if the task type is
				// changed during workflow construction or editing, but we need it that way so we don't nuke old
				// values if task type changes back.)
				const sanitizedConfig = new Map<string, string>();
				const keys = step.configuration.keys();
				for (const key of keys) {
					if (template.configuration.has(key)) {
						sanitizedConfig.set(key, step.configuration.get(key));
					}
				}

				// We also check here for system and user defaults, and nuke them if present. They
				// get filled in when we want to run the workflow, we never store these values
				// in the database with the workflow.
				step.configuration.forEach((_value, key) => {
					// System and user defaults are stored in the form "Task Name::Property Name", so
					// we need to build that up to find our keys.
					const fullKey = step.name + '::' + key;
					if (defaults && defaults.has(fullKey)) {
						sanitizedConfig.set(key, '');
					}
				});

				step.configuration = sanitizedConfig;
			}
		});

		const outputTemplate = outputTaskTemplates.find(template => template.name === workflow.outputStep.name);
		if (outputTemplate) {
			const sanitizedConfig = new Map<string, string>();
			const keys = workflow.outputStep.configuration.keys();
			for (const key of keys) {
				if (outputTemplate.configuration.has(key)) {
					sanitizedConfig.set(key, workflow.outputStep.configuration.get(key));
				}
			}
			workflow.outputStep.configuration = sanitizedConfig;
		}
	}

	private objectify(workflow: any): Workflow {
		const w: Workflow = {
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
		};
		return w;
	}
}