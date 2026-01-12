import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from 'src/app/user.service';
import { forkJoin } from 'rxjs';
import { WorkflowService } from '../workflow.service';
import { TaskSpecification, OutputTaskSpecification } from 'src/app/model/workflow/task-specification';
import { Workflow } from 'src/app/model/workflow/workflow';
import { AlertService } from 'src/app/alert.service';
import { WorkflowEditorComponent } from './workflow-editor.component';

@Component({
	selector: 'minty-run-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'run-workflow.component.html',
	styleUrls: []
})
export class RunWorkflowComponent implements OnInit {

	defaults: Map<string, string>;
	workflow: Workflow;
	taskSpecifications: TaskSpecification[];
	outputTaskSpecifications: OutputTaskSpecification[];

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
				this.defaults = new Map([...systemDefaults, ...userDefaults]);
				this.taskSpecifications = taskSpecifications;
				this.outputTaskSpecifications = outputTaskSpecifications;

				// This is nested inside to ensure that all required information is loaded before the actual workflow.
				this.workflowService.getWorkflow(params['id']).subscribe((workflow: Workflow) => {
					workflow.steps.forEach(step => {
						const updated = step.configuration;
						for (const key of Object.keys(step.configuration)) {
							if (this.defaults?.has(key)) {
								updated.set(key, this.defaults.get(key));
							}
						}
						step.configuration = updated;
					});
					this.workflow = workflow;
				});

			});
		});
		
	}

	runWorkflow() {
		this.workflowService.sanitize(this.workflow, this.taskSpecifications, this.defaults);

		this.workflowService.execute(this.workflow).subscribe((result: string) => {
			this.alertService.postSuccess(result);
		});
		this.router.navigateByUrl('workflow');
	}

	cancel() {
		this.router.navigateByUrl('workflow');
	}

}
