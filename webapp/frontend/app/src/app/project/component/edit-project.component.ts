import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Project } from 'src/app/model/project/project';
import { ProjectService } from 'src/app/project/project.service';
import { ProjectEditorComponent } from './project-editor.component';
import { NodeInfo } from 'src/app/model/project/node-info';

@Component({
	selector: 'minty-edit-project',
	imports: [CommonModule, FormsModule, ProjectEditorComponent],
	templateUrl: 'edit-project.component.html',
	styleUrls: []
})
export class EditProjectComponent implements OnInit {

	project: Project;

	constructor(private route: ActivatedRoute,
		private router: Router,
		private projectService: ProjectService) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			this.projectService.getProject(params['id']).subscribe((project: Project) => {
				this.project = project;
			});
		});
	}

	done() {
		this.router.navigateByUrl('projects');
	}

}
