import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { WorkflowService } from '../workflow.service';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { WorkflowTask } from '../../model/workflow-task';

@Component({
    selector: 'ai-workflow-list',
    imports: [CommonModule, FormsModule, RouterModule ],
    templateUrl: 'workflow-list.component.html',
    styleUrls: ['workflow.component.css']
})
export class WorkflowListComponent implements OnInit, OnDestroy {

    responseText: unknown;
    results: string[] = [];
    tasks: WorkflowTask[] = [];
    triggerTasks: WorkflowTask[] = [];
    private subscription: Subscription;

    constructor(private router: Router,
        private workflowService: WorkflowService) {
        this.subscription = this.workflowService.resultList$.subscribe((value: string[]) => {
            this.results = value;
        });
    }

    ngOnInit() {
        this.workflowService.listWorkflowTasks().subscribe((tasks) => {
            this.tasks = tasks;
        });
        this.workflowService.listTriggeredWorkflowTasks().subscribe((triggerTasks) => {
            this.triggerTasks = triggerTasks;
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    displayResultsFor(result: string) {
        this.responseText = '';
        this.workflowService.getResult(result).subscribe(result => {
            this.responseText = result;
        });
    }

    deleteTask(task: WorkflowTask) {
        this.workflowService.deleteTask(task).subscribe((tasks) => {
            this.tasks = tasks;
        });
    }

    deleteResult(result: string) {
        this.workflowService.deleteResult(result).subscribe();
    }

    copyToClipboard() {
        navigator.clipboard.writeText(this.responseText as string).then(() => {
            console.log('Copied: ' + this.responseText as string);
        });
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }

    navigateToTask(taskId: number): void {
        this.router.navigate(['workflow/', taskId]);
    }
}
