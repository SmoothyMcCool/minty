
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router, RouterModule } from "@angular/router";
import { ProjectService } from "../project.service";
import { Project } from "../../model/project/project";
import { ConfirmationDialogComponent } from "../../app/component/confirmation-dialog.component";
import { User } from "../../model/user";
import { UserService } from "../../user.service";
import { ProjectEditorComponent } from "./project-editor.component";

@Component({
	selector: 'minty-project-list',
	imports: [FormsModule, RouterModule, ConfirmationDialogComponent, ProjectEditorComponent],
	templateUrl: 'project-list.component.html'
})
export class ProjectListComponent implements OnInit {

	user: User | null = null;

	projects: Project[] = [];
	activeProject: Project | undefined = undefined;

	confirmDeleteProjectVisible = false;
	projectPendingDeletion: Project | undefined = undefined;

	newProjectVisible = false;
	newProjectName = '';

	constructor(private router: Router,
		private userService: UserService,
		private projectService: ProjectService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe((user: User) => {
			this.user = user;
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
	openProject(project: Project) {
		this.activeProject = project;
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

}
