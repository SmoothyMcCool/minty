import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TaskService } from '../task.service';
import { Subscription } from 'rxjs';
import { Router, RouterModule } from '@angular/router';
import { Task } from '../../model/task';

@Component({
    selector: 'ai-task-list',
    imports: [CommonModule, FormsModule, RouterModule ],
    templateUrl: 'task-list.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class TaskListComponent implements OnInit, OnDestroy {

    responseText: unknown;
    results: string[] = [];
    tasks: Task[] = [];
    triggerTasks: Task[] = [];
    private subscription: Subscription;

    constructor(private router: Router,
        private TaskService: TaskService) {
        this.subscription = this.TaskService.resultList$.subscribe((value: string[]) => {
            this.results = value;
        });
    }

    ngOnInit() {
        this.TaskService.listTasks().subscribe((tasks) => {
            this.tasks = tasks;
        });
        this.TaskService.listTriggeredTasks().subscribe((triggerTasks) => {
            this.triggerTasks = triggerTasks;
        });
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    displayResultsFor(result: string) {
        this.responseText = '';
        this.TaskService.getResult(result).subscribe(result => {
            this.responseText = result;
        });
    }

    deleteTask(task: Task) {
        this.TaskService.deleteTask(task).subscribe((tasks) => {
            this.tasks = tasks;
        });
    }

    deleteResult(result: string) {
        this.TaskService.deleteResult(result).subscribe();
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
        this.router.navigate(['task/', taskId]);
    }
}
