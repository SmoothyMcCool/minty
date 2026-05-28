import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ProjectService } from '../project.service';
import { Project } from '../../model/project/project';
import { FormsModule } from '@angular/forms';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { ProjectEditorComponent } from './project-editor.component';

@Component({
	selector: 'minty-view-project',
	imports: [FormsModule, RouterModule, ConfirmationDialogComponent, ProjectEditorComponent],
	templateUrl: 'view-project.component.html'
})
export class ViewProjectComponent {
	activeProject: Project | undefined = undefined;

	constructor(private projectService: ProjectService) {
	}

	ngOnInit() {
		this.projectService.activeProject$.subscribe(activeProject => {
			this.activeProject = activeProject;
		});
	}

}
