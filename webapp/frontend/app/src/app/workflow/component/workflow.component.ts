import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertService } from 'src/app/alert.service';
import { Workflow } from 'src/app/model/workflow/workflow';
import { WorkflowService } from 'src/app/workflow/workflow.service';
import { TaskTemplateService } from 'src/app/task/task-template.service';
import { TaskDescription } from 'src/app/model/task-description';
import { WorkflowEditorComponent } from './workflow-editor.component';

@Component({
	selector: 'minty-workflow',
	imports: [CommonModule, FormsModule, WorkflowEditorComponent],
	templateUrl: 'workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class WorkflowComponent implements OnInit {

	workflow: Workflow = {
		id: '',
		ownerId: '',
		name: '',
		description: '',
		shared: false,
		workflowSteps: [],
		outputStep: {
			name: '',
			configuration: new Map()
		}
	};
	taskTemplates: TaskDescription[] = [];
	outputTaskTemplates: TaskDescription[] = [];

	constructor(
		private router: Router,
		private route: ActivatedRoute,
		private workflowService: WorkflowService,
		private taskTemplateService: TaskTemplateService,
		private alertService: AlertService) {
	}

	ngOnInit(): void {
		const workflowId = this.route.snapshot.paramMap.get('id');

		this.workflowService.getWorkflow(workflowId).subscribe((workflow: Workflow) => {
			this.workflow = workflow;

			this.taskTemplateService.listTemplates().subscribe(templates => {
				this.taskTemplates = templates;
			});

			this.taskTemplateService.listOutputTemplates().subscribe(output => {
				this.outputTaskTemplates = output;
			});

		});
	}

	submit() {
		this.workflowService.execute(this.workflow).subscribe((result: string) => {
			this.alertService.postSuccess(result);
		});
		this.router.navigateByUrl('workflow');
	}

	getInputsFor(taskName: string): string {
		const task = this.taskTemplates.find(element => element.name === taskName);
		if (task) {
			return task.inputs;
		}
		return "";
	}

	getOutputsFor(taskName: string): string {
		const task = this.taskTemplates.find(element => element.name === taskName);
		if (task) {
			return task.outputs;
		}
		return "";
	}

	navigateTo(url: string) {
		this.router.navigateByUrl(url);
	}
}
