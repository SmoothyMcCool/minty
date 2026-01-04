import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { ProjectService } from "../project.service";
import { Project } from "src/app/model/project/project";
import { ConfirmationDialogComponent } from "src/app/app/component/confirmation-dialog.component";

@Component({
	selector: 'minty-project-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'project-list.component.html'
})
export class ProjectListComponent implements OnInit {

	projects: Project[];

	confirmDeleteProjectVisible = false;
	projectPendingDeletion: Project;

	constructor(private router: Router,
		private projectService: ProjectService) {
	}

	ngOnInit() {
		this.projectService.listProjects().subscribe((projects: Project[]) => {
			this.projects = projects;
		});
	}

	newProject() {
		this.projectService.createProject().subscribe(() => {
			this.ngOnInit();
		})
	}

	navigateToProject(id: string) {
		this.router.navigate(['/projects', id]);
	}

	deleteProject(project: Project) {
		this.projectPendingDeletion = project;
		this.confirmDeleteProjectVisible = true;
	}

	confirmDeleteProject() {
		this.confirmDeleteProjectVisible = false;
		this.projectService.deleteProject(this.projectPendingDeletion.id).subscribe(() => {
			this.projectService.listProjects().subscribe((projects: Project[]) => {
				this.projects = projects;
			});
		});
	}
}
