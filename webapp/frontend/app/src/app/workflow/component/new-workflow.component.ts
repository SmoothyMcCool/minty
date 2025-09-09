import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AlertService } from 'src/app/alert.service';
import { TaskTemplateService } from 'src/app/task/task-template.service';
import { Workflow } from 'src/app/model/workflow/workflow';
import { WorkflowService } from '../workflow.service';
import { TaskDescription } from 'src/app/model/task-description';
import { WorkflowEditorComponent } from './workflow-editor.component';
import { ResultTemplate } from 'src/app/model/workflow/result-template';
import { UserService } from 'src/app/user.service';
import { forkJoin } from 'rxjs';

@Component({
	selector: 'minty-new-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'new-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class NewWorkflowComponent implements OnInit {

	taskTemplates: TaskDescription[] = [];
	outputTaskTemplates: TaskDescription[] = [];
	showTemplateUploader = false;
	defaults: Map<string, string>;

	resultTemplate: ResultTemplate = {
		id: '',
		name: '',
		file: undefined
	};

	workflow: Workflow = {
		name: '',
		description: '',
		id: '',
		ownerId: '',
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
		private taskTemplateService: TaskTemplateService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.taskTemplateService.listTemplates().subscribe((taskTemplates: TaskDescription[]) => {
			this.taskTemplates = taskTemplates;
		});
		this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: TaskDescription[]) => {
			this.outputTaskTemplates = outputTaskTemplates;
		});

		forkJoin({
			systemDefaults: this.userService.systemDefaults(),
			userDefaults: this.userService.userDefaults()
		}).subscribe(({ systemDefaults, userDefaults }) => {
			// User defaults should take priority in conflicts.
			this.defaults = new Map([ ...systemDefaults, ...userDefaults ]);
		});

	}

	createWorkflow() {
		this.workflowService.sanitize(this.workflow, this.taskTemplates, this.outputTaskTemplates, this.defaults);

		this.workflowService.newWorkflow(this.workflow).subscribe(() => {
			this.alertService.postSuccess('Workflow Created!');
			this.router.navigateByUrl('workflow');
		});
	}

	cancel() {
		this.router.navigateByUrl('workflow');
	}

	fileChanged(event: Event) {
		const newFile = (event.target as HTMLInputElement).files;
		if (newFile && newFile.length == 1) {
			this.resultTemplate.file = newFile[0];
		}
	}

	uploadFile() {
		if (!this.resultTemplate.file || !this.resultTemplate.name) {
				this.alertService.postFailure('You have to give a title and actual file, k?');
				return;
			}

			this.workflowService.addResultTemplate(this.resultTemplate).subscribe((message) => {
				this.alertService.postSuccess(message);
				this.showTemplateUploader = false;
				this.resultTemplate = {
					id: '',
					name: '',
					file: undefined
				};
			});
	}
}
