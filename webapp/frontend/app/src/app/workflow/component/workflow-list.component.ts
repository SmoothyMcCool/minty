import { AfterViewChecked, Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { animate, style, transition, trigger } from '@angular/animations';
import { WorkflowService } from '../workflow.service';
import * as bootstrap from 'bootstrap';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { UserSelectDialogComponent, UserSelection } from '../../app/component/user-select-dialog.component';
import { User } from '../../model/user';
import { Workflow, WorkflowDescription } from '../../model/workflow/workflow';
import { WorkflowExecutionState } from '../../model/workflow/workflow-execution-state';
import { WorkflowResult } from '../../model/workflow/workflow-result';
import { WorkflowState } from '../../model/workflow/workflow-state';
import { UserService } from '../../user.service';
import { ResultService } from '../result.service';

@Component({
	selector: 'minty-workflow-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent, UserSelectDialogComponent],
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
export class WorkflowListComponent implements AfterViewChecked, OnInit, OnDestroy {

	responseType: string | undefined = undefined;
	currentResult: WorkflowResult  | undefined = undefined;
	workflowStatus: WorkflowExecutionState | undefined = undefined;
	results: WorkflowState[] = [];
	workflows: WorkflowDescription[] = [];
	private subscription: Subscription | undefined = undefined;

	pendingWorkflow: Workflow | undefined = undefined;
	confirmWorkflowDeleteVisible = false;
	confirmResultDeleteVisible = false;
	resultPendingDeletionId: string | undefined = undefined;
	confirmResultDuplicateWorkflowVisible = false;
	confirmCancelWorkflowVisible = false;
	confirmDeleteAllResultsVisible = false;
	sortOrder: string = 'alpha';

	user!: User;

	filter: string | undefined = undefined;
	displayResults: WorkflowState[] = [];

	userSelectDialogVisible = false;
	workflowToShare: Workflow | undefined = undefined;
	sharingSelection: UserSelection | undefined = undefined;

	constructor(private router: Router,
		private alertService: AlertService,
		private workflowService: WorkflowService,
		private resultService: ResultService,
		private userService: UserService) {
	}

	ngAfterViewChecked() {
		const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
		Array.from(tooltipTriggerList).forEach(el => {
			const existing = bootstrap.Tooltip.getInstance(el);
			if (!existing) {
				new bootstrap.Tooltip(el);
			}
		});
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;

			this.subscription = this.resultService.workflowResultList$.subscribe((value: WorkflowState[]) => {
				const map = new Map(value.map(i => [i.name, i]));

				for (const [id, item] of map) {
					const index = this.results.findIndex(i => i.name === id);
					if (index !== -1) {
						this.results[index] = item;
					} else {
						this.results.push(item);
					}
				}

				this.results = this.results.filter(i => map.has(i.name));
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
		this.currentResult = undefined;
		this.resultService.openWorkflowOutput(result.id);
	}

	downloadResultsFor(result:WorkflowState) {
		this.currentResult = undefined;
		this.resultService.downloadWorkflowOutput(result.id);
	}

	downloadLogsFor(result: WorkflowState) {
		this.currentResult = undefined;
		this.resultService.downloadWorkflowLog(result.id);
	}

	deleteWorkflow(workflow: Workflow) {
		this.pendingWorkflow = workflow;
		this.confirmWorkflowDeleteVisible = true;
	}

	confirmDeleteWorkflow() {
		if (this.pendingWorkflow) {
			this.confirmWorkflowDeleteVisible = false;
			this.workflowService.deleteWorkflow(this.pendingWorkflow.id!).subscribe(() => {
				this.workflowService.listWorkflows().subscribe((workflows) => {
					this.workflows = workflows;
				});
			});
			this.workflows = this.workflows.filter(item => item.id === this.pendingWorkflow!.id);
		} else {
			console.error('confirmDeleteWorkflow: pendingWorkflow not set');
		}
	}

	displayProgress(result: WorkflowState) {
		this.workflowStatus = result.state;
	}

	cancelWorkflow(workflow: Workflow) {
		this.confirmCancelWorkflowVisible = true;
		this.pendingWorkflow = workflow;
	}

	confirmCancelWorkflow() {
		if (this.pendingWorkflow) {
			this.confirmCancelWorkflowVisible = false;
			this.workflowService.cancelWorkflow(this.pendingWorkflow.name).subscribe(() => {
				this.workflowService.listWorkflows().subscribe((workflows) => {
					this.workflows = workflows;
				});
			});
		} else {
			console.error('confirmCancelWorkflow: pendingWorkflow not set');
		}
	}

	hideProgress() {
		this.workflowStatus = undefined;
	}

	deleteResult(event: MouseEvent, result: WorkflowState) {
		event.stopPropagation();
		this.confirmResultDeleteVisible = true;
		this.resultPendingDeletionId = result.id;
	}

	confirmDeleteResult() {
		if (this.resultPendingDeletionId) {
			this.confirmResultDeleteVisible = false;
			this.resultService.deleteWorkflowResult(this.resultPendingDeletionId).subscribe();
			this.results = this.results.filter(item => item.id != this.resultPendingDeletionId);
			this.filterChanged(this.filter);
		} else {
			console.error('resultPendingDeletionId not set');
		}
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

	shareWorkflow(workflow: Workflow): void {
		this.userSelectDialogVisible = true;
		this.workflowToShare = workflow;
		this.workflowService.getSharingList(workflow.name).subscribe(selection => {
			this.sharingSelection = selection;
		});
	}

	onUsersConfirmed(selection: UserSelection): void {
		if (this.workflowToShare) {
			this.userSelectDialogVisible = false;
			this.workflowService.shareWorkflow(this.workflowToShare.id!, selection).subscribe(response => {
				this.alertService.postSuccess(response);
				this.workflowToShare = undefined;
			});
		} else {
			console.log('workflowToShare is undefined');
		}
	}

	copyToClipboard() {
		if (this.currentResult) {
			navigator.clipboard.writeText(this.currentResult.output as string).then(() => {
				console.log('Copied: ' + this.currentResult!.output as string);
			});
		} else {
			console.log('curerntResult is undefined')
		}
	}

	viewFullscreen() {
		if (this.currentResult) {
		this.router.navigate(['/workflow/result', this.currentResult.id]);
		} else {
			console.log('curerntResult is undefined')
		}
	}

	isOwned(workflow: WorkflowDescription): boolean {
		return workflow.owned;
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
		let w = this.workflows.find(w => w.id === this.pendingWorkflow!.id);
		if (w && w.id) {
			this.workflowService.getWorkflow(w.id).subscribe(workflow => {
				this.workflowService.newWorkflow(workflow).subscribe(workflow => {
					this.router.navigate(['/workflow/edit', workflow.id]);
				});
			})
		} else {
			this.alertService.postFailure("Failed to duplicate workflow.");
		}
	}

	downloadWorkflow(workflow: Workflow) {
		this.workflowService.getWorkflow(workflow.id).subscribe(workflowToDownload => {
			workflowToDownload.id = '';
			workflowToDownload.owned = true;
			const blob = new Blob([JSON.stringify(workflowToDownload, undefined, 2)], { type: 'application/json' });
			const url = URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = workflow.name + '.json';
			link.style.display = 'none';
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		});

	}

	filterChanged(filter: string | undefined) {
		this.filter = filter;
		if (this.filter) {
			this.displayResults = this.results.filter(result => result.name.includes(filter!));
		} else {
			this.displayResults = this.results;
		}
	}

}
