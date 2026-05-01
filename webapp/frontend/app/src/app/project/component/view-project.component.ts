import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProjectService } from '../project.service';
import { interval, startWith, Subscription, switchMap } from 'rxjs';

@Component({
	selector: 'minty-view-project',
	imports: [CommonModule, FormsModule, RouterModule],
	templateUrl: 'view-project.component.html'
})
export class ViewProjectComponent implements OnInit, OnDestroy {

	subscription: Subscription | undefined;
	tasks: string[] = [];
	anyTaskCompleted: boolean = false;

	public constructor(private projectService: ProjectService) {
	}

	public ngOnInit(): void {
		this.subscription = interval(5000)
			.pipe(
				startWith(0), // fires immediately, then every 5s
				switchMap(() => this.projectService.listTasks())
			)
			.subscribe(tasks => {
				const removed = this.tasks.filter(
					task => !tasks.some(t => t === task)
				);
				if (removed.length > 0) {
					this.anyTaskCompleted = true;
				}
				this.tasks = tasks;
			});
	}

	public ngOnDestroy(): void {
		this.subscription?.unsubscribe();
	}
}
