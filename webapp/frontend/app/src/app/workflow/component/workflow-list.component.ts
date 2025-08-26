import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { ResultService } from 'src/app/workflow/result.service';
import { WorkflowService } from '../workflow.service';
import { Workflow } from 'src/app/model/workflow/workflow';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserService } from 'src/app/user.service';
import { WorkflowState } from 'src/app/model/workflow/workflow-state';

@Component({
	selector: 'minty-workflow-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'workflow-list.component.html',
	styleUrls: ['workflow.component.css']
})
export class WorkflowListComponent implements OnInit, OnDestroy {

	responseType: string;
	responseText: unknown;
	results: WorkflowState[] = [];
	workflows: Workflow[] = [];
	private subscription: Subscription;

	confirmWorkflowDeleteVisible = false;
	confirmResultDeleteVisible = false;
	workflowPendingDeletion: Workflow;
	resultPendingDeletionId: string;

	constructor(private router: Router,
		private workflowService: WorkflowService,
		private resultService: ResultService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.subscription = this.resultService.workflowResultList$.subscribe((value: WorkflowState[]) => {
			this.results = value;
		});
		this.workflowService.listWorkflows().subscribe((workflows) => {
			this.workflows = workflows;
		});
	}

	ngOnDestroy(): void {
		if (this.subscription) {
			this.subscription.unsubscribe();
			this.subscription = undefined;
		}
	}

	displayResultsFor(result: WorkflowState) {
		this.responseText = '';
		this.resultService.getWorkflowResult(result.id).subscribe(result => {
			if (this.endsWithIgnoreCase(result.outputFormat, 'text/json')) {
				this.responseType = 'JSON';
			} else if (this.endsWithIgnoreCase(result.outputFormat, 'text/html')) {
				this.responseType = 'HTML';
			} else {
				this.responseType = 'TEXT';
			}
			this.responseText = result.output;
		});
	}

	private endsWithIgnoreCase(str: string, ending: string) {
		return str.toLowerCase().endsWith(ending.toLowerCase());
	}

	deleteWorkflow(workflow: Workflow) {
		this.workflowPendingDeletion = workflow;
		this.confirmWorkflowDeleteVisible = true;
	}

	confirmDeleteWorkflow() {
		this.confirmWorkflowDeleteVisible = false;
		this.workflowService.deleteWorkflow(this.workflowPendingDeletion.id).subscribe(() => {
			this.workflowService.listWorkflows().subscribe((workflows) => {
				this.workflows = workflows;
			});
		});
		this.workflows = this.workflows.filter(item => item.id === this.workflowPendingDeletion.id);
	}

	deleteResult(event: MouseEvent, result: WorkflowState) {
		event.stopPropagation();
		this.confirmResultDeleteVisible = true;
		this.resultPendingDeletionId = result.id;
	}

	confirmDeleteResult() {
		this.confirmResultDeleteVisible = false;
		this.resultService.deleteWorkflowResult(this.resultPendingDeletionId).subscribe();
		this.results = this.results.filter(item => item.id != this.resultPendingDeletionId);
	}

	copyToClipboard() {
		navigator.clipboard.writeText(this.responseText as string).then(() => {
			console.log('Copied: ' + this.responseText as string);
		});
	}

	isOwned(workflow: Workflow): boolean {
		return workflow.ownerId === this.userService.getUser().id;
	}

	navigateTo(url: string) {
		this.router.navigateByUrl(url);
	}

	navigateToWorkflow(taskId: number): void {
		this.router.navigate(['workflow/', taskId]);
	}

	editWorkflow(workflowId: string) {
		this.router.navigate(['/workflow/edit', workflowId]);
	}

}
