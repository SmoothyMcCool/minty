import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { ProjectService } from "../project.service";
import { Project } from "../../model/project/project";
import { ConfirmationDialogComponent } from "../../app/component/confirmation-dialog.component";
import { User } from "../../model/user";
import { UserService } from "../../user.service";

@Component({
	selector: 'minty-project-list',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'project-list.component.html'
})
export class ProjectListComponent implements OnInit {

	user: User | null = null;

	projects: Project[] = [];

	confirmDeleteProjectVisible = false;
	projectPendingDeletion: Project | undefined = undefined;

	newProjectVisible = false;
	newProjectName = '';

	activeProject: string | undefined = undefined;

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

	downloadProject(project: Project) {
		this.projectService.downloadProjectZip(project.id);
	}

	deleteProject(project: Project) {
		this.projectPendingDeletion = project;
		this.confirmDeleteProjectVisible = true;
	}

	confirmDeleteProject() {
		this.confirmDeleteProjectVisible = false;
		this.projectService.deleteProject(this.projectPendingDeletion!.id).subscribe(() => {
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
