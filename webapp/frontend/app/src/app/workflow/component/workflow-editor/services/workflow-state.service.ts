import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin } from 'rxjs';
import { AssistantService } from '../../../../assistant.service';
import { DocumentService } from '../../../../document.service';
import { MintyDoc } from '../../../../model/minty-doc';
import { MintyTool } from '../../../../model/minty-tool';
import { Model } from '../../../../model/model';
import { EnumList } from '../../../../model/workflow/enum-list';
import { AttributeMap, TaskSpecification, OutputTaskSpecification, TaskRequest } from '../../../../model/workflow/task-specification';
import { Workflow } from '../../../../model/workflow/workflow';
import { ToolService } from '../../../../tool.service';
import { UserService } from '../../../../user.service';
import { WorkflowService } from '../../../workflow.service';

@Injectable({ providedIn: 'root' })
export class WorkflowStateService {

	private _workflow = new BehaviorSubject<Workflow | null>(null);
	workflow$ = this._workflow.asObservable();

	enumLists: EnumList[] = [];
	models: Model[] = [];
	documents: MintyDoc[] = [];
	tools: MintyTool[] = [];
	defaults: AttributeMap = {};
	taskSpecifications: TaskSpecification[] = [];
	outputTaskSpecifications: OutputTaskSpecification[] = [];

	get workflow(): Workflow | null {
		return this._workflow.getValue();
	}

	public constructor(private workflowService: WorkflowService,
		private assistantService: AssistantService,
		private documentService: DocumentService,
		private toolService: ToolService,
		private userService: UserService,
	) {
		forkJoin({
			models: this.assistantService.models(),
			documents: this.documentService.list(),
			tools: this.toolService.list(),
			enumLists: this.workflowService.listEnumLists(),
			taskSpecifications: this.workflowService.listTaskSpecifications(),
			outputTaskSpecifications: this.workflowService.listOutputTaskSpecifications(),
		}).subscribe(({ models, documents, tools, enumLists, taskSpecifications, outputTaskSpecifications }) => {
			this.models = models;
			this.documents = documents;
			this.tools = tools;
			this.enumLists = enumLists;
			this.taskSpecifications = taskSpecifications;
			this.outputTaskSpecifications = outputTaskSpecifications;
			this.defaults = { ...this.userService.getSystemDefaults(), ...this.userService.getUserDefaults() };
		});
	}

	setWorkflow(workflow: Workflow) {
		this._workflow.next(workflow);
	}

	updateWorkflow(updater: (workflow: Workflow) => void) {
		const current = this._workflow.getValue();
		if (!current) {
			return;
		}
		updater(current);
		this._workflow.next({ ...current });
	}

	getTaskById(stepId: string): TaskRequest {
		const ret = this.workflow?.steps.find(step => step.id === stepId);
		if (!ret) {
			throw new Error('Step not found');
		}
		return ret;
	}
}