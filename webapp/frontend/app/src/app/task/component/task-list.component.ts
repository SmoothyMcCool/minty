import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TaskService } from '../task.service';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { ResultService } from 'src/app/result.service';
import { StandaloneTask } from 'src/app/model/standalone-task';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserService } from 'src/app/user.service';

@Component({
    selector: 'minty-task-list',
    imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
    templateUrl: 'task-list.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class TaskListComponent implements OnInit, OnDestroy {

    responseText: unknown;
    results: string[] = [];
    tasks: StandaloneTask[] = [];
    triggerTasks: StandaloneTask[] = [];

    confirmResultDeleteVisible = false;
    resultPendingDeletionId: string;

    confirmTaskDeleteVisible = false;
    taskPendingDeletion: StandaloneTask;

    private subscription: Subscription;

    constructor(private router: Router,
        private taskService: TaskService,
        private resultService: ResultService,
        private userService: UserService) {
    }

    ngOnInit() {
        this.subscription = this.resultService.taskResultList$.subscribe((value: string[]) => {
            this.results = value;
        });
        this.taskService.listTasks().subscribe((tasks) => {
            this.tasks = tasks;
        });
        this.taskService.listTriggeredTasks().subscribe((triggerTasks) => {
            this.triggerTasks = triggerTasks;
        });
    }

    ngOnDestroy(): void {
        if (this.subscription !== undefined) {
            this.subscription.unsubscribe();
            this.subscription = undefined;
        }
    }

    displayResultsFor(result: string) {
        this.responseText = '';
        this.resultService.getTaskResult(result).subscribe(result => {
            this.responseText = result;
        });
    }

    deleteTask(task: StandaloneTask) {
        this.taskPendingDeletion = task;
        this.confirmTaskDeleteVisible = true;
    }

    confirmDeleteTask() {
        this.confirmTaskDeleteVisible = false;
        this.taskService.deleteTask(this.taskPendingDeletion).subscribe((tasks) => {
            this.tasks = tasks;
        });
        this.tasks = this.tasks.filter(item => item.id === this.taskPendingDeletion.id);
    }

    deleteResult(event: MouseEvent, result: string) {
        event.stopPropagation();
        this.resultPendingDeletionId = result;
        this.confirmResultDeleteVisible = true;
    }

    confirmDeleteResult() {
        this.confirmResultDeleteVisible = false;
        this.resultService.deleteTaskResult(this.resultPendingDeletionId).subscribe();
        this.results = this.results.filter(item => item === this.resultPendingDeletionId);
    }

    copyToClipboard() {
        navigator.clipboard.writeText(this.responseText as string).then(() => {
            console.log('Copied: ' + this.responseText as string);
        });
    }

    isOwned(task: StandaloneTask): boolean {
        return task.ownerId === this.userService.getUser().id;
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }

    navigateToTask(taskId: number): void {
        this.router.navigate(['task/', taskId]);
    }
}
