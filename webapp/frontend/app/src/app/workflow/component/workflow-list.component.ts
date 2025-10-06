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
import { WorkflowResult } from 'src/app/model/workflow/workflow-result';
import { WorkflowExecutionState } from 'src/app/model/workflow/workflow-execution-state';
import { animate, style, transition, trigger } from '@angular/animations';
import { User } from 'src/app/model/user';

@Component({
	selector: 'minty-workflow-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'workflow-list.component.html',
	styleUrls: ['workflow.component.css'],
	animations: [
		trigger('slideInOut', [
			transition(':leave', [
				style({ transform: 'translateX(0)', opacity: 1 }),
				animate('300ms ease', style({ transform: 'translateX(100%)', opacity: 0 }))
			]),
			transition(':enter', [
				style({ transform: 'translateX(100%)', opacity: 0 }),
				animate('300ms ease', style({ transform: 'translateX(0)', opacity: 1 }))
			])
		])
	]
})
export class WorkflowListComponent implements OnInit, OnDestroy {

	responseType: string;
	currentResult: WorkflowResult = null;
	workflowStatus: WorkflowExecutionState;
	results: WorkflowState[] = [];
	workflows: Workflow[] = [];
	private subscription: Subscription;

	confirmWorkflowDeleteVisible = false;
	confirmResultDeleteVisible = false;
	workflowPendingDeletion: Workflow;
	resultPendingDeletionId: string;

	user: User;

	constructor(private router: Router,
		private workflowService: WorkflowService,
		private resultService: ResultService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
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
		this.currentResult = null;
		this.resultService.openWorkflowOutput(result.id);
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

	displayProgress(result: WorkflowState) {
		this.workflowStatus = result.state;
	}

	hideProgress() {
		this.workflowStatus = null;
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
		navigator.clipboard.writeText(this.currentResult.output as string).then(() => {
			console.log('Copied: ' + this.currentResult.output as string);
		});
	}

	viewFullscreen() {
		this.router.navigate(['/workflow/result', this.currentResult.id]);
	}

	isOwned(workflow: Workflow): boolean {
		return workflow.ownerId === this.user.id;
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
