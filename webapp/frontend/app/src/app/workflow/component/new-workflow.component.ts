import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AlertService } from 'src/app/alert.service';
import { UserService } from 'src/app/user.service';
import { forkJoin } from 'rxjs';
import { WorkflowService } from '../workflow.service';
import { Workflow } from 'src/app/model/workflow/workflow';
import { OutputTaskSpecification, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { ResultTemplate } from 'src/app/model/workflow/result-template';
import { WorkflowEditorComponent } from './workflow-editor.component';

@Component({
	selector: 'minty-new-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'new-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class NewWorkflowComponent implements OnInit {

	taskSpecifications: TaskSpecification[] = [];
	outputTaskSpecifications: OutputTaskSpecification[];
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
		steps: [],
		outputStep: undefined,
		connections: []
	};

	constructor(private alertService: AlertService,
		private router: Router,
		private workflowService: WorkflowService,
		private userService: UserService) {
	}

	ngOnInit() {

		forkJoin({
			systemDefaults: this.userService.systemDefaults(),
			userDefaults: this.userService.userDefaults(),
			taskSpecifications: this.workflowService.listTaskSpecifications(),
			outputTaskSpecifications: this.workflowService.listOutputTaskSpecifications()
		}).subscribe(({ systemDefaults, userDefaults, taskSpecifications, outputTaskSpecifications }) => {
			// User defaults should take priority in conflicts.
			this.defaults = new Map([ ...systemDefaults, ...userDefaults ]);
			this.taskSpecifications = taskSpecifications;
			this.outputTaskSpecifications = outputTaskSpecifications;
		});

	}

	createWorkflow() {
		this.workflowService.sanitize(this.workflow, this.taskSpecifications, this.defaults);

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

	workflowUpdated(workflow: Workflow) {
		this.workflow = workflow;
	}
}
