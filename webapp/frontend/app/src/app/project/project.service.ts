import { Injectable } from "@angular/core";
import { catchError, EMPTY, map, Observable } from "rxjs";
import { Project } from "../model/project/project";
import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";
import { NodeInfo } from "../model/project/node-info";
import { Node } from "../model/project/node";

@Injectable({
	providedIn: 'root'
})

export class ProjectService {

	private static readonly CreateProject = 'api/project/create';
	private static readonly DeleteProject = 'api/project/delete';
	private static readonly GetProject = 'api/project';
	private static readonly ListProjects = 'api/project/list';
	private static readonly ListNodes = 'api/project/node/list';
	private static readonly GetNode = 'api/project/node';
	private static readonly DeleteNode = 'api/project/node';
	private static readonly AddNode = 'api/project/node';

	public constructor(private http: HttpClient, private alertService: AlertService) {
	}

	createProject(name: string): Observable<Project> {
		const headers: HttpHeaders = new HttpHeaders({
			'Content-Type': 'application/json'
		});

		const project: Project = {
			id: '',
			name: name
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

	listProjectEntries(projectId: string): Observable<NodeInfo[]> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);

		return this.http.get<ApiResult>(ProjectService.ListNodes, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as NodeInfo[];
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

	getNode(projectId: string, entry: NodeInfo): Observable<Node> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);
		params = params.append('nodeId',  entry.nodeId);

		return this.http.get<ApiResult>(ProjectService.GetNode, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Node;
				})
			);
	}

	addOrUpdateNode(projectId: string, entry: Node): Observable<boolean> {
		const form = new FormData();
		form.append('projectId', projectId);
		form.append('node', new Blob([JSON.stringify(entry)], { type: "application/json" }));

		return this.http.post<ApiResult>(ProjectService.AddNode, form)
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

	deleteNode(projectId: string, entry: NodeInfo): Observable<boolean> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);
		params = params.append('node',  entry.nodeId);

		return this.http.delete<ApiResult>(ProjectService.DeleteNode, { params: params })
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