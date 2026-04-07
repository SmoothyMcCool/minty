import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowService } from '../workflow.service';
import { WorkflowEditorComponent } from './workflow-editor/workflow-editor.component';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { AttributeMap } from '../../model/workflow/task-specification';
import { Workflow } from '../../model/workflow/workflow';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-edit-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent, ConfirmationDialogComponent],
	templateUrl: 'edit-workflow.component.html',
	styleUrls: []
})
export class EditWorkflowComponent implements OnInit {

	defaults!: AttributeMap;
	workflow: Workflow | undefined;
	logLevel: string = "Debug";
	unsavedChanges = false;
	isNew = false;
	uploadDialogVisible = false;
	uploadedWorkflow: Workflow | undefined;

	confirmCancelVisible = false;

	constructor(private route: ActivatedRoute,
		private router: Router,
		private alertService: AlertService,
		private workflowService: WorkflowService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			// User defaults should take priority in conflicts.
			this.defaults = { ...this.userService.getSystemDefaults(), ...this.userService.getUserDefaults() };

			// This is nested inside to ensure that all required information is loaded before the actual workflow.
			const workflowId = params['id'];
			if (workflowId) {
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
			}
			this.isNew = this.route.snapshot.queryParamMap.get('new') === 'true';
			if (this.isNew) {
				this.workflow = {
					id: null,
					owned: true,
					name: '',
					description: '',
					steps: [],
					connections: [],
					outputStep: undefined
				};
			}

		});
	}

	updateWorkflow() {
		if (!this.workflow) {
			console.error('updateWorkflow: workflow not set');
			return;
		}

		this.workflowService.sanitize(this.workflow);

		if (this.isNew) {
			this.workflowService.newWorkflow(this.workflow).subscribe((w: Workflow) => {
				this.alertService.postSuccess('Workflow Created!');
				this.unsavedChanges = false;
				this.isNew = false;
				this.workflow = w;
			});
		} else {
			this.workflowService.updateWorkflow(this.workflow).subscribe(() => {
				this.alertService.postSuccess('Workflow Updated!');
				this.unsavedChanges = false;
			});
		}
	}

	runWorkflow() {
		if (!this.workflow) {
			console.error('runWorkflow: workflow not set');
			return;
		}

		this.workflowService.sanitize(this.workflow);

		this.workflowService.execute(this.workflow, this.logLevel).subscribe((result: string) => {
			this.alertService.postSuccess(result);
		});
		this.router.navigateByUrl('workflow');
	}

	cancel() {
		if (!this.unsavedChanges) {
			this.confirmCancel();
		}
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
		this.unsavedChanges = true;
	}

	showUploadWorkflowDialog() {
		this.uploadedWorkflow = undefined;
		this.uploadDialogVisible = true;
	}

	fileSelected(event: Event) {
		const fileList = (event.target as HTMLInputElement).files;
		if (fileList && fileList.length > 0) {
			const file = fileList[0];
			const reader = new FileReader();
			reader.onload = (e) => {
				this.uploadedWorkflow = JSON.parse(e.target?.result as string);
				console.log(this.uploadedWorkflow);
			};
			reader.readAsText(file);
		}
	}

	uploadWorkflow() {
		if (this.uploadedWorkflow) {
			this.workflow = this.uploadedWorkflow;
		} else {
			console.error('uploadWorkflow: uploadedWorkflow not set');
		}
		this.uploadedWorkflow = undefined;
		this.uploadDialogVisible = false;
	}
}
