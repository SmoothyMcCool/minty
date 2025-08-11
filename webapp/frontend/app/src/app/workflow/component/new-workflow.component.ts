import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AlertService } from "src/app/alert.service";
import { TaskTemplateService } from "src/app/task/task-template.service";
import { Workflow } from "src/app/model/workflow";
import { WorkflowService } from "../workflow.service";
import { TaskDescription } from "src/app/model/task-description";
import { WorkflowEditorComponent } from "./workflow-editor.component";

@Component({
	selector: 'minty-new-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'new-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class NewWorkflowComponent implements OnInit {

	taskTemplates: TaskDescription[] = [];
	outputTaskTemplates: TaskDescription[] = [];

	workflow: Workflow = {
		name: '',
		description: '',
		id: 0,
		ownerId: 0,
		shared: false,
		workflowSteps: [],
		outputStep: {
			name: '',
			configuration: new Map()
		}
	};

	isFileTriggered: boolean = false;
	triggerDirectory: string = '';

	configParams = new Map<string, string>();
	outputTaskConfigParams = new Map<string, string>();

	constructor(private alertService: AlertService,
		private router: Router,
		private workflowService: WorkflowService,
		private taskTemplateService: TaskTemplateService) {
	}

	ngOnInit() {
		this.taskTemplateService.listTemplates().subscribe((taskTemplates: TaskDescription[]) => {
			this.taskTemplates = taskTemplates;
		});
		this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: TaskDescription[]) => {
			this.outputTaskTemplates = outputTaskTemplates;
		});
	}

	createWorkflow() {
		this.workflowService.newWorkflow(this.workflow).subscribe(() => {
			this.alertService.postSuccess('Workflow Created!');
			this.router.navigateByUrl('workflow');
		});
	}

	cancel() {
		this.router.navigateByUrl('workflow');
	}
}
