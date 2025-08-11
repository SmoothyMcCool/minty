import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { AlertService } from "src/app/alert.service";
import { TaskTemplateService } from "src/app/task/task-template.service";
import { Workflow } from "src/app/model/workflow";
import { WorkflowService } from "../workflow.service";
import { TaskDescription } from "src/app/model/task-description";
import { WorkflowEditorComponent } from "./workflow-editor.component";

@Component({
	selector: 'minty-edit-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'edit-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class EditWorkflowComponent implements OnInit {

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
		private route: ActivatedRoute,
		private router: Router,
		private workflowService: WorkflowService,
		private taskTemplateService: TaskTemplateService) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			this.workflowService.getWorkflow(params['id']).subscribe((workflow: Workflow) => {
				this.workflow = workflow;
			});
		});
		this.taskTemplateService.listTemplates().subscribe((taskTemplates: TaskDescription[]) => {
			this.taskTemplates = taskTemplates;
		});
		this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: TaskDescription[]) => {
			this.outputTaskTemplates = outputTaskTemplates;
		});
	}

	updateWorkflow() {
		this.workflowService.updateWorkflow(this.workflow).subscribe(() => {
			this.alertService.postSuccess('Workflow Updated!');
			this.router.navigateByUrl('workflow');
		});
	}

	cancel() {
		this.router.navigateByUrl('workflow');
	}
}
