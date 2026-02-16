import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from 'src/app/user.service';
import { forkJoin } from 'rxjs';
import { Workflow } from 'src/app/model/workflow/workflow';
import { WorkflowService } from '../workflow.service';
import { OutputTaskSpecification, AttributeMap, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { AlertService } from 'src/app/alert.service';
import { WorkflowEditorComponent } from './workflow-editor.component';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';

@Component({
	selector: 'minty-edit-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent, ConfirmationDialogComponent],
	templateUrl: 'edit-workflow.component.html',
	styleUrls: []
})
export class EditWorkflowComponent implements OnInit {

	defaults: AttributeMap;
	workflow: Workflow;
	taskSpecifications: TaskSpecification[];
	outputTaskSpecifications: OutputTaskSpecification[];

	confirmCancelVisible = false;

	constructor(private route: ActivatedRoute,
		private router: Router,
		private alertService: AlertService,
		private workflowService: WorkflowService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			forkJoin({
				systemDefaults: this.userService.systemDefaults(),
				userDefaults: this.userService.userDefaults(),
				taskSpecifications: this.workflowService.listTaskSpecifications(),
				outputTaskSpecifications: this.workflowService.listOutputTaskSpecifications()
			}).subscribe(({ systemDefaults, userDefaults, taskSpecifications, outputTaskSpecifications }) => {

				// User defaults should take priority in conflicts.
				this.defaults = { ...systemDefaults, ...userDefaults };
				this.taskSpecifications = taskSpecifications;
				this.taskSpecifications = taskSpecifications;
				this.outputTaskSpecifications = outputTaskSpecifications;

				// This is nested inside to ensure that all required information is loaded before the actual workflow.
				this.workflowService.getWorkflow(params['id']).subscribe((workflow: Workflow) => {
					workflow.steps.forEach(step => {
						const updated = step.configuration;
						for (const key of Object.keys(step.configuration)) {
							if (this.defaults && key in this.defaults) {
								updated[key] = this.defaults[key];
							}
						}
						step.configuration = updated;
					});
					this.workflow = workflow;
				});

			});
		});

	}

	updateWorkflow() {
		this.workflowService.sanitize(this.workflow, this.taskSpecifications, this.defaults);

		this.workflowService.updateWorkflow(this.workflow).subscribe(() => {
			this.alertService.postSuccess('Workflow Updated!');
			this.router.navigateByUrl('workflow');
		});
	}

	cancel() {
		this.confirmCancelVisible = true;
	}

	confirmCancel() {
		this.router.navigateByUrl('workflow');
	}

	workflowUpdated(workflow: Workflow) {
		this.workflow = {
			...workflow,
			steps: workflow.steps.map(step => ({
				...step,
				configuration: { ...step.configuration },
				layout: { ...step.layout }
			})),
			connections: workflow.connections.map(conn => ({ ...conn }))
		};
	}

}
