import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { ProjectService } from "../project.service";
import { Project } from "src/app/model/project/project";
import { ConfirmationDialogComponent } from "src/app/app/component/confirmation-dialog.component";
import { UserService } from "src/app/user.service";
import { User } from "src/app/model/user";

@Component({
	selector: 'minty-project-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'project-list.component.html'
})
export class ProjectListComponent implements OnInit {

	user: User | null = null;

	projects: Project[] = [];

	confirmDeleteProjectVisible = false;
	projectPendingDeletion: Project;

	newProjectVisible = false;
	newProjectName = '';

	activeProject: string;

	constructor(private router: Router,
		private userService: UserService,
		private projectService: ProjectService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe((user: User) => {
			this.user = user;
			this.activeProject = user.defaults['defaultProject'];
		});
		this.projectService.listProjects().subscribe((projects: Project[]) => {
			this.projects = projects;
		});
	}

	createNewProject() {
		this.newProjectVisible = false;
		this.projectService.createProject(this.newProjectName).subscribe(() => {
			this.ngOnInit();
		})
	}

	cancelNewProject() {
		this.newProjectVisible = false;
		this.newProjectName = '';
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

	setActive(project: Project) {
		if (this.user) {
			this.user.defaults['defaultProject'] = project.id;
			this.userService.update(this.user).subscribe(() => {
				this.activeProject = project.id;
			});
		}
	}
}
