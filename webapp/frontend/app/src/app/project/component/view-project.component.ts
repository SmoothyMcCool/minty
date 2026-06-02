import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProjectService } from '../project.service';
import { Project } from '../../model/project/project';
import { FormsModule } from '@angular/forms';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { ProjectEditorComponent } from './project-editor.component';
import { Subscription } from 'rxjs';

@Component({
	selector: 'minty-view-project',
	imports: [FormsModule, RouterModule, ConfirmationDialogComponent, ProjectEditorComponent],
	templateUrl: 'view-project.component.html'
})
export class ViewProjectComponent implements OnInit, OnDestroy {
	activeProject: Project | undefined = undefined;
	activeProjectSubscription: Subscription | undefined = undefined;

	constructor(private projectService: ProjectService) {
	}

	ngOnInit() {
		this.projectService.listProjects().subscribe();
		this.projectService.activeProject$.subscribe(activeProject => {
			this.activeProject = activeProject;
		});
	}

	ngOnDestroy(): void {
		this.activeProjectSubscription?.unsubscribe();
	}
}
