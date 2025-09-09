import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertService } from 'src/app/alert.service';
import { TaskTemplateService } from 'src/app/task/task-template.service';
import { WorkflowService } from '../workflow.service';
import { TaskDescription } from 'src/app/model/task-description';
import { WorkflowEditorComponent } from './workflow-editor.component';
import { Workflow } from 'src/app/model/workflow/workflow';
import { UserService } from 'src/app/user.service';
import { forkJoin } from 'rxjs';

@Component({
	selector: 'minty-edit-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'edit-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class EditWorkflowComponent implements OnInit {

	taskTemplates: TaskDescription[] = [];
	outputTaskTemplates: TaskDescription[] = [];
	defaults: Map<string, string>;

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
		private route: ActivatedRoute,
		private router: Router,
		private workflowService: WorkflowService,
		private taskTemplateService: TaskTemplateService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			this.workflowService.getWorkflow(params['id']).subscribe((workflow: Workflow) => {
				this.workflow = workflow;
			});

			forkJoin({
				systemDefaults: this.userService.systemDefaults(),
				userDefaults: this.userService.userDefaults()
			}).subscribe(({ systemDefaults, userDefaults }) => {
				// User defaults should take priority in conflicts.
				this.defaults = new Map([ ...systemDefaults, ...userDefaults ]);
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
		this.workflowService.sanitize(this.workflow, this.taskTemplates, this.outputTaskTemplates, this.defaults);

		this.workflowService.updateWorkflow(this.workflow).subscribe(() => {
			this.alertService.postSuccess('Workflow Updated!');
			this.router.navigateByUrl('workflow');
		});
	}

	cancel() {
		this.router.navigateByUrl('workflow');
	}
}
