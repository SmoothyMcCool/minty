import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin } from 'rxjs';
import { AssistantService } from 'src/app/assistant.service';
import { DocumentService } from 'src/app/document.service';
import { MintyDoc } from 'src/app/model/minty-doc';
import { MintyTool } from 'src/app/model/minty-tool';
import { Model } from 'src/app/model/model';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { AttributeMap, OutputTaskSpecification, TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { Workflow } from 'src/app/model/workflow/workflow';
import { ToolService } from 'src/app/tool.service';
import { UserService } from 'src/app/user.service';
import { WorkflowService } from 'src/app/workflow/workflow.service';

@Injectable({ providedIn: 'root' })
export class WorkflowStateService {

	private _workflow = new BehaviorSubject<Workflow | null>(null);
	workflow$ = this._workflow.asObservable();

	enumLists: EnumList[];
	models: Model[];
	documents: MintyDoc[];
	tools: MintyTool[];
	defaults: AttributeMap;
	taskSpecifications: TaskSpecification[] = [];
	outputTaskSpecifications: OutputTaskSpecification[] = [];

	get workflow(): Workflow {
		return this._workflow.getValue();
	}

	public constructor(private workflowService: WorkflowService,
		private assistantService: AssistantService,
		private documentService: DocumentService,
		private toolService: ToolService,
		private userService: UserService,
	) { }

	setWorkflow(workflow: Workflow) {
		this._workflow.next(workflow);

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

	updateWorkflow(updater: (workflow: Workflow) => void) {
		const current = this._workflow.getValue();
		if (!current) {
			return;
		}
		updater(current);
		this._workflow.next({ ...current });
	}

	getTaskById(stepId: string): TaskRequest {
		return this.workflow.steps.find(step => step.id === stepId);
	}
}