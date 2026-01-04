import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { ResultService } from 'src/app/workflow/result.service';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserService } from 'src/app/user.service';
import { WorkflowState } from 'src/app/model/workflow/workflow-state';
import { WorkflowResult } from 'src/app/model/workflow/workflow-result';
import { WorkflowExecutionState } from 'src/app/model/workflow/workflow-execution-state';
import { animate, style, transition, trigger } from '@angular/animations';
import { User } from 'src/app/model/user';
import { AlertService } from 'src/app/alert.service';
import { Workflow } from 'src/app/model/workflow/workflow';
import { WorkflowService } from '../workflow.service';

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

	pendingWorkflow: Workflow;
	confirmWorkflowDeleteVisible = false;
	confirmResultDeleteVisible = false;
	resultPendingDeletionId: string;
	confirmResultDuplicateWorkflowVisible = false;
	confirmCancelWorkflowVisible = false;
	confirmDeleteAllResultsVisible = false;
	sortOrder: string = 'alpha';

	user: User;

	filter: string;
	displayResults: WorkflowState[] = [];

	constructor(private router: Router,
		private alertService: AlertService,
		private workflowService: WorkflowService,
		private resultService: ResultService,
		private userService: UserService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;

			this.subscription = this.resultService.workflowResultList$.subscribe((value: WorkflowState[]) => {
				const map = new Map(value.map(i => [i.id, i]));

				for (const [id, item] of map) {
					const index = this.results.findIndex(i => i.id === id);
					if (index !== -1) {
						this.results[index] = item;
					} else {
						this.results.push(item);
					}
				}

				this.results = this.results.filter(i => map.has(i.id));
				this.filterChanged(this.filter);
			});

			this.workflowService.listWorkflows().subscribe((workflows) => {
				this.workflows = workflows;
			});
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

	downloadResultsFor(result:WorkflowState) {
		this.currentResult = null;
		this.resultService.downloadWorkflowOutput(result.id);
	}

	downloadLogsFor(result: WorkflowState) {
		this.currentResult = null;
		this.resultService.downloadWorkflowLog(result.id);
	}

	deleteWorkflow(workflow: Workflow) {
		this.pendingWorkflow = workflow;
		this.confirmWorkflowDeleteVisible = true;
	}

	confirmDeleteWorkflow() {
		this.confirmWorkflowDeleteVisible = false;
		this.workflowService.deleteWorkflow(this.pendingWorkflow.id).subscribe(() => {
			this.workflowService.listWorkflows().subscribe((workflows) => {
				this.workflows = workflows;
			});
		});
		this.workflows = this.workflows.filter(item => item.id === this.pendingWorkflow.id);
	}

	displayProgress(result: WorkflowState) {
		this.workflowStatus = result.state;
	}

	cancelWorkflow(workflow: Workflow) {
		this.confirmCancelWorkflowVisible = true;
		this.pendingWorkflow = workflow;
	}

	confirmCancelWorkflow() {
		this.confirmCancelWorkflowVisible = false;
		this.workflowService.cancelWorkflow(this.pendingWorkflow.name).subscribe(() => {
			this.workflowService.listWorkflows().subscribe((workflows) => {
				this.workflows = workflows;
			});
		});
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
		this.filterChanged(this.filter);
	}

	deleteAllVisibleResults(event: MouseEvent) {
		event.stopPropagation();
		this.confirmDeleteAllResultsVisible = true;
	}

	confirmDeleteAllVisibleResults() {
		this.confirmDeleteAllResultsVisible = false;
		const resultsToDelete = this.displayResults;
		for (const result of resultsToDelete) {
			this.resultPendingDeletionId = result.id;
			this.resultService.deleteWorkflowResult(result.id).subscribe();
			this.results = this.results.filter(item => item.id != this.resultPendingDeletionId);
			this.filterChanged(this.filter);
		}
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

	editWorkflow(workflow: Workflow) {
		this.router.navigate(['/workflow/edit', workflow.id]);
	}

	duplicateWorkflow(workflow: Workflow) {
		this.pendingWorkflow = workflow;
		this.confirmResultDuplicateWorkflowVisible = true;
	}

	confirmDuplicateWorkflow() {
		this.confirmResultDuplicateWorkflowVisible = false;
		let w = this.workflows.find(w => w.id === this.pendingWorkflow.id);
		if (w) {
			w.id = null;
			this.workflowService.newWorkflow(w).subscribe(workflow => {
				this.router.navigate(['/workflow/edit', workflow.id]);
			});
		} else {
			this.alertService.postFailure("Failed to duplicate workflow.");
		}
	}

	filterChanged(filter: string) {
		this.filter = filter;
		if (this.filter) {
			this.displayResults = this.results.filter(result => result.name.includes(filter));
		} else {
			this.displayResults = this.results;
		}
	}

}
