import { Injectable } from "@angular/core";
import { catchError, EMPTY, map, Observable } from "rxjs";
import { Project } from "../model/project/project";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";
import { ProjectEntryInfo } from "../model/project/project-entry-info";
import { ProjectEntry } from "../model/project/project-entry";

@Injectable({
	providedIn: 'root'
})

export class ProjectService {

	private static readonly CreateProject = 'api/project/create';
	private static readonly DeleteProject = 'api/project/delete';
	private static readonly GetProject = 'api/project';
	private static readonly ListProjects = 'api/project/list';
	private static readonly ListProjectEntries = 'api/project/entries';
	private static readonly GetProjectEntry = 'api/project/entry';
	private static readonly DeleteProjectEntry = 'api/project/entry';
	private static readonly AddProjectEntry = 'api/project/entry';

	public constructor(private http: HttpClient, private alertService: AlertService) {
	}

	createProject(): Observable<Project> {
		const headers: HttpHeaders = new HttpHeaders({
			'Content-Type': 'application/json'
		});

		const project: Project = {
			id: '',
			name: crypto.randomUUID()
		};
		
		return this.http.post<ApiResult>(ProjectService.CreateProject, project, { headers: headers })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Project;
				})
			);
	}

	deleteProject(projectId: string): Observable<boolean> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);

		return this.http.delete<ApiResult>(ProjectService.DeleteProject, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as boolean;
				})
			);
	}

	getProject(projectId: string): Observable<Project> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);

		return this.http.get<ApiResult>(ProjectService.GetProject, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Project;
				})
			);
	}

	listProjectEntries(projectId: string): Observable<ProjectEntryInfo[]> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);

		return this.http.get<ApiResult>(ProjectService.ListProjectEntries, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as ProjectEntryInfo[];
				})
			);
	}

	listProjects(): Observable<Project[]> {
		return this.http.get<ApiResult>(ProjectService.ListProjects)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Project[];
				})
			);
	}

	getProjectEntry(projectId: string, entry: ProjectEntryInfo): Observable<ProjectEntry> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);
		params = params.append('entry',  entry.id);

		return this.http.get<ApiResult>(ProjectService.GetProjectEntry, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as ProjectEntry;
				})
			);
	}

	addOrUpdateProjectEntry(projectId: string, entry: ProjectEntry): Observable<boolean> {
		const form = new FormData();
		form.append('projectId', projectId);
		form.append('entry', new Blob([JSON.stringify(entry)], { type: "application/json" }));

		return this.http.post<ApiResult>(ProjectService.AddProjectEntry, form)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as boolean;
				})
			);
	}

	deleteProjectEntry(projectId: string, entry: ProjectEntryInfo): Observable<boolean> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);
		params = params.append('entry',  entry.id);

		return this.http.delete<ApiResult>(ProjectService.DeleteProjectEntry, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as boolean;
				})
			);
	}

}