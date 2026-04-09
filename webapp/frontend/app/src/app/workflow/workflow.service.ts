import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../alert.service';
import { catchError, EMPTY, map, Observable, ReplaySubject, take } from 'rxjs';
import { ApiResult } from '../model/api-result';
import { EnumList } from '../model/workflow/enum-list';
import { OutputTaskSpecification, AttributeMap, TaskRequest, TaskSpecification } from '../model/workflow/task-specification';
import { Workflow, WorkflowDescription } from '../model/workflow/workflow';
import { UserService } from '../user.service';
import { UserSelection } from '../app/component/user-select-dialog.component';

@Injectable({
	providedIn: 'root'
})
export class WorkflowService {

	private static readonly GetWorkflow = 'api/workflow'; // Retrieve a workflow by ID
	private static readonly DeleteWorkflow = 'api/workflow'; // Delete a workflow
	private static readonly CancelWorkflow = 'api/workflow/cancel'; // Cancel an in progress a workflow
	private static readonly ListWorkflows = 'api/workflow/list'; // Retrieve all workflows
	private static readonly ExecuteWorkflow = 'api/workflow/execute'; // Run a workflow
	private static readonly NewWorkflow = 'api/workflow/new'; // Create a new workflow
	private static readonly UpdateWorkflow = 'api/workflow/update'; // Update an existing workflow
	private static readonly ShareWorkflow = 'api/workflow/share';
	private static readonly ListSharedUsers = 'api/workflow/getsharing';
	private static readonly ListTaskSpecifications = 'api/workflow/specification/list';
	private static readonly ListOutputTaskSpecifications = 'api/workflow/output/specification/list';
	private static readonly ListEnumLists = 'api/workflow/enum';
	private static readonly GetTaskHelpFiles = 'api/workflow/help/task';
	private static readonly GetOutputHelpFiles = 'api/workflow/help/output';

	private taskSpecifications$ = new ReplaySubject<TaskSpecification[]>(1);
	private taskSpecificationsSnapshot: TaskSpecification[] = [];
	private outputTaskSpecifications$ = new ReplaySubject<OutputTaskSpecification[]>(1);
	private outputTaskSpecificationsSnapshot: OutputTaskSpecification[] = [];
	private enumLists$ = new ReplaySubject<EnumList[]>(1);

	constructor(private http: HttpClient, private alertService: AlertService, private userService: UserService) {
		this.http.get<ApiResult>(WorkflowService.ListTaskSpecifications)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					const returnValue = result.data as TaskSpecification[];
					returnValue.forEach(value => {
						value.configuration = { ...value.configuration };
						value.configSpec = { ...value.configSpec };
					});

					return returnValue;
				})
			).subscribe(specs => {
				this.taskSpecifications$.next(specs);
				this.taskSpecificationsSnapshot = specs;
			});

		this.http.get<ApiResult>(WorkflowService.ListOutputTaskSpecifications)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					const returnValue = result.data as OutputTaskSpecification[];
					returnValue.forEach(value => {
						value.configuration = { ...value.configuration };
						value.configSpec = { ...value.configSpec };
					});

					return returnValue;
				})
			).subscribe(specs => {
				this.outputTaskSpecifications$.next(specs);
				this.outputTaskSpecificationsSnapshot = specs;
			});

		this.http.get<ApiResult>(WorkflowService.ListEnumLists)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as EnumList[];
				})
			).subscribe(lists => this.enumLists$.next(lists));
	}

	listTaskSpecifications(): Observable<TaskSpecification[]> {
		return this.taskSpecifications$.pipe(take(1));
	}

	listOutputTaskSpecifications(): Observable<OutputTaskSpecification[]> {
		return this.outputTaskSpecifications$.pipe(take(1));
	}

	listTaskNames(): string[] {
		return this.taskSpecificationsSnapshot.map(item => item.taskName);
	}

	listOutputTaskNames(): string[] {
		return this.outputTaskSpecificationsSnapshot.map(item => item.taskName);
	}

	getTaskSpecification(name: string): TaskSpecification | undefined {
		return this.taskSpecificationsSnapshot.find(item => item.taskName.localeCompare(name) === 0);
	}

	getOutputTaskSpecification(name: string): OutputTaskSpecification | undefined {
		return this.outputTaskSpecificationsSnapshot.find(item => item.taskName.localeCompare(name) === 0);
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
					return this.sortWorkflows(Array.from(result.data as any[]).map(element => this.objectify(element))) as Workflow[];
				})
			);
	}

	cancelWorkflow(name: string): Observable<void> {
		let params: HttpParams = new HttpParams();
		params = params.append('name', name);

		return this.http.delete<ApiResult>(WorkflowService.CancelWorkflow, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return;
				})
			);
	}

	newWorkflow(workflow: Workflow): Observable<Workflow> {
		let outputStep = null;
		if (workflow.outputStep) {
			outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				loggingActive: workflow.outputStep.loggingActive,
				configuration: { ...workflow.outputStep.configuration },
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}

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
					loggingActive: item.loggingActive,
					configuration: { ...item.configuration },
					layout: {
						x: item.layout.x,
						y: item.layout.y,
						numInputs: item.layout.numInputs,
						numOutputs: item.layout.numOutputs
					}
				};
			}),
			outputStep: outputStep
		};

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
		let outputStep = null;
		if (workflow.outputStep) {
			outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				loggingActive: workflow.outputStep.loggingActive,
				configuration: { ...workflow.outputStep.configuration },
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}

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
					loggingActive: item.loggingActive,
					configuration: { ...item.configuration },
					layout: {
						x: item.layout.x,
						y: item.layout.y,
						numInputs: item.layout.numInputs,
						numOutputs: item.layout.numOutputs
					}
				};
			}),
			outputStep: outputStep
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

	listWorkflows(): Observable<WorkflowDescription[]> {
		return this.http.get<ApiResult>(WorkflowService.ListWorkflows)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return this.sortWorkflows(Array.from(result.data as WorkflowDescription[]));
				})
			);
	}

	shareWorkflow(name: string, userSelection: UserSelection): Observable<string> {
		const body = {
			resource: name,
			userSelection: userSelection
		}
		return this.http.post<ApiResult>(WorkflowService.ShareWorkflow, body)
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

	getSharingList(name: string): Observable<UserSelection> {
		let params: HttpParams = new HttpParams();
		params = params.append('name', name);

		return this.http.get<ApiResult>(WorkflowService.ListSharedUsers, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as UserSelection;
				})
			);
	}

	execute(workflow: Workflow, logLevel: string): Observable<string> {
		const body = {
			id: workflow.id,
			logLevel: logLevel,
			taskConfigurationList: workflow.steps.map(step => step.configuration),
			outputConfiguration: workflow.outputStep ? workflow.outputStep.configuration : null
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

	listEnumLists(): Observable<EnumList[]> {
		return this.enumLists$.pipe(take(1));
	}

	getTaskHelpFiles(): Observable<Map<string, string>> {
		return this.http.get<ApiResult>(WorkflowService.GetTaskHelpFiles)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return new Map(Object.entries(result.data as any));
				})
			);
	}

	getOutputHelpFiles(): Observable<Map<string, string>> {
		return this.http.get<ApiResult>(WorkflowService.GetOutputHelpFiles)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return new Map(Object.entries(result.data as any));
				})
			);
	}

	sanitize(workflow: Workflow) {
		// User defaults should take priority in conflicts.
		const defaults = { ...this.userService.getSystemDefaults(), ...this.userService.getUserDefaults() };

		workflow.steps.forEach(step => {
			const spec = this.taskSpecificationsSnapshot.find(spec => spec.taskName === step.taskName);
			if (spec) {
				// This removes any keys that should not be present (which happens sometimes if the task type is
				// changed during workflow construction or editing, but we need it that way so we don't nuke old
				// values if task type changes back.)
				let sanitizedConfig: AttributeMap = {};
				const keys = Object.keys(step.configuration);
				if (spec.configuration) {
					for (const key of keys) {
						if (key in spec.configuration) {
							sanitizedConfig[key] = step.configuration[key];
						}
					}
				}

				// We also check here for system and user defaults, and nuke them if present. They
				// get filled in when we want to run the workflow, we never store these values
				// in the database with the workflow.
				for (const key of Object.keys(step.configuration)) {
					// System and user defaults are stored in the form "Task Name::Property Name", so
					// we need to build that up to find our keys.
					const fullKey = step.taskName + '::' + key;
					if (defaults && fullKey in defaults) {
						sanitizedConfig[key] = '';
					}
				}

				step.configuration = sanitizedConfig;
			}
		});
	}

	private objectify(workflow: any): Workflow {
		let outputStep = undefined;
		if (workflow.outputStep) {
			outputStep = {
				taskName: workflow.outputStep.taskName,
				stepName: workflow.outputStep.stepName,
				id: workflow.outputStep.id,
				loggingActive: workflow.outputStep.loggingActive,
				configuration: workflow.outputStep.configuration,
				layout: {
					x: workflow.outputStep.layout.x,
					y: workflow.outputStep.layout.y,
					numInputs: workflow.outputStep.layout.numInputs,
					numOutputs: workflow.outputStep.layout.numOutputs
				}
			}
		}

		const w: Workflow = {
			id: workflow.id,
			owned: workflow.owned,
			name: workflow.name,
			description: workflow.description,
			steps: (workflow.steps as any[]).map((element: TaskRequest) => {
				return {
					taskName: element.taskName,
					stepName: element.stepName,
					id: element.id,
					loggingActive: element.loggingActive,
					configuration: element.configuration,
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
			outputStep: outputStep
		};

		return w;
	}

	private sortWorkflows(workflows: Workflow[] | WorkflowDescription[]) : Workflow[] | WorkflowDescription[] {
		return workflows.sort((left, right) => {
			if (!left.name && !right.name) {
				if (!left.id) {
					return 1;
				}
				if (!right.id) {
					return -1;
				}
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