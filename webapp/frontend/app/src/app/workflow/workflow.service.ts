import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../alert.service';
import { catchError, EMPTY, map, Observable } from 'rxjs';
import { ApiResult } from '../model/api-result';
import { ResultTemplate } from '../model/workflow/result-template';
import { EnumList } from '../model/workflow/enum-list';
import { OutputTaskSpecification, TaskRequest, TaskSpecification } from '../model/workflow/task-specification';
import { Workflow } from '../model/workflow/workflow';

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
	private static readonly ListTaskSpecifications = 'api/workflow/specification/list';
	private static readonly ListOutputTaskSpecifications = 'api/workflow/output/specification/list';
	private static readonly ListEnumLists = 'api/workflow/enum';


	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	listTaskSpecifications(): Observable<TaskSpecification[]> {
		return this.http.get<ApiResult>(WorkflowService.ListTaskSpecifications)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					const returnValue = result.data as TaskSpecification[];
					returnValue.forEach(value => {
						value.configuration = new Map(Object.entries(value.configuration));
						value.configSpec = new Map(Object.entries(value.configSpec));
					});

					return returnValue;
				})
			);
	}

	listOutputTaskSpecifications(): Observable<OutputTaskSpecification[]> {
		return this.http.get<ApiResult>(WorkflowService.ListOutputTaskSpecifications)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					const returnValue = result.data as OutputTaskSpecification[];
					returnValue.forEach(value => {
						value.configuration = new Map(Object.entries(value.configuration));
						value.configSpec = new Map(Object.entries(value.configSpec));
					});

					return returnValue;
				})
			);
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

	newWorkflow(workflow: Workflow): Observable<Workflow> {
		const body = {
			id: workflow.id,
			name: workflow.name,
			description: workflow.description,
			connections: workflow.connections,
			steps: workflow.steps.map(item => {
				return {
					taskName: item.taskName,
					stepName: item.stepName,
					id: item.id,
					configuration: Object.fromEntries(item.configuration),
					layout: {
						x: item.layout.x,
						y: item.layout.y,
						numInputs: item.layout.numInputs,
						numOutputs: item.layout.numOutputs
					}
				};
			}),
			outputStep: null
		};
		if (workflow.outputStep) {
			body.outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				configuration: Object.fromEntries(workflow.outputStep.configuration),
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}

		return this.http.post<ApiResult>(WorkflowService.NewWorkflow, body)
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

	updateWorkflow(workflow: Workflow): Observable<Workflow> {
		const body = {
			id: workflow.id,
			name: workflow.name,
			description: workflow.description,
			connections: workflow.connections,
			steps: workflow.steps.map(item => {
				return {
					taskName: item.taskName,
					stepName: item.stepName,
					id: item.id,
					configuration: Object.fromEntries(item.configuration),
					layout: {
						x: item.layout.x,
						y: item.layout.y,
						numInputs: item.layout.numInputs,
						numOutputs: item.layout.numOutputs
					}
				};
			}),
			outputStep: null
		};
		if (workflow.outputStep) {
			body.outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				configuration: Object.fromEntries(workflow.outputStep.configuration),
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}

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
			taskConfigurationList: workflow.steps.map(step => Object.fromEntries(step.configuration)),
			outputConfiguration: workflow.outputStep ? Object.fromEntries(workflow.outputStep.configuration) : null
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

	listEnumLists(): Observable<EnumList[]> {
		return this.http.get<ApiResult>(WorkflowService.ListEnumLists)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as EnumList[];
				})
			);
	}

	sanitize(workflow: Workflow, taskSpecifications: TaskSpecification[], defaults: Map<string, string>) {
		workflow.steps.forEach(step => {
			const spec = taskSpecifications.find(spec => spec.taskName === step.taskName);
			if (spec) {
				// This removes any keys that should not be present (which happens sometimes if the task type is
				// changed during workflow construction or editing, but we need it that way so we don't nuke old
				// values if task type changes back.)
				const sanitizedConfig = new Map<string, string>();
				const keys = step.configuration.keys();
				for (const key of keys) {
					if (spec.configuration.has(key)) {
						sanitizedConfig.set(key, step.configuration.get(key));
					}
				}

				// We also check here for system and user defaults, and nuke them if present. They
				// get filled in when we want to run the workflow, we never store these values
				// in the database with the workflow.
				step.configuration.forEach((_value, key) => {
					// System and user defaults are stored in the form "Task Name::Property Name", so
					// we need to build that up to find our keys.
					const fullKey = step.taskName + '::' + key;
					if (defaults && defaults.has(fullKey)) {
						sanitizedConfig.set(key, '');
					}
				});

				step.configuration = sanitizedConfig;
			}
		});
	}

	private objectify(workflow: any): Workflow {
		const w: Workflow = {
			id: workflow.id,
			ownerId: workflow.ownerId,
			name: workflow.name,
			shared: workflow.shared,
			description: workflow.description,
			steps: (workflow.steps as any[]).map((element: TaskRequest) => {
				return {
					taskName: element.taskName,
					stepName: element.stepName,
					id: element.id,
					configuration: new Map(Object.entries(element.configuration)),
					layout: {
						x: element.layout.x,
						y: element.layout.y,
						numInputs: element.layout.numInputs,
						numOutputs: element.layout.numOutputs
					}
				};
			}),
			connections: (workflow.connections as any[]).map(element => {
				return {
					readerId: element.readerId,
					readerPort: element.readerPort,
					writerId: element.writerId,
					writerPort: element.writerPort
				};
			}),
			outputStep: null
		};
		if (workflow.outputStep) {
			w.outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				configuration: workflow.outputStep.configuration instanceof Map 
					? new Map(workflow.outputStep.configuration)
					: new Map(Object.entries(workflow.outputStep.configuration ?? {})),
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}
		return w;
	}
}