import { Injectable } from "@angular/core";
import { catchError, EMPTY, map, Observable } from "rxjs";
import { HttpClient, HttpParams } from "@angular/common/http";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";
import { Project } from "../model/project/project";
import { ProjectNode } from "../model/project/project-node";
import { DocProperties } from "../document/document-editor.component";

@Injectable({
	providedIn: 'root'
})
export class ProjectService {

	// -------------------------
	// ROUTES
	// -------------------------

	private static readonly CreateProject = 'api/project/create';
	private static readonly DeleteProject = 'api/project/delete';
	private static readonly GetProject = 'api/project';
	private static readonly ListProjects = 'api/project/list';

	private static readonly ReadNode = 'api/project/node';
	private static readonly WriteFile = 'api/project/node/file';
	private static readonly ConvertToMarkdownAndAddFile = 'api/project/node/convert/markdown';
	private static readonly ConvertToMarkdownDecomposeFile = 'api/project/node/convert/markdown/decompose';
	private static readonly ConvertToMarkdownDecomposeAndSummarizeFile = 'api/project/node/convert/markdown/summarize';
	private static readonly ConvertToMermaid = 'api/project/node/convert/mermaid';
	private static readonly ExportZip = 'api/project/node/export/zip';
	private static readonly ImportZip = 'api/project/node/import/zip';
	private static readonly CreateFolder = 'api/project/node/folder';
	private static readonly MoveNode = 'api/project/node/move';
	private static readonly DeleteNode = 'api/project/node';
	private static readonly UpdateNodeMeta = 'api/project/node/meta';

	private static readonly DescribeTree = 'api/project/node/tree';
	private static readonly ListChildren = 'api/project/node/children';

	constructor(private http: HttpClient,
		private alertService: AlertService) { }

	// -------------------------
	// PROJECTS
	// -------------------------

	createProject(name: string): Observable<Project> {

		const project: Project = {
			id: '',
			name: name
		};

		return this.http.post<ApiResult>(ProjectService.CreateProject, project).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as Project)
		);
	}

	deleteProject(projectId: string): Observable<boolean> {

		const params = new HttpParams()
			.set('projectId', projectId);

		return this.http.delete<ApiResult>(ProjectService.DeleteProject, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as boolean)
		);
	}

	getProject(projectId: string): Observable<Project> {

		const params = new HttpParams()
			.set('projectId', projectId);

		return this.http.get<ApiResult>(ProjectService.GetProject, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as Project)
		);
	}

	listProjects(): Observable<Project[]> {

		return this.http.get<ApiResult>(ProjectService.ListProjects).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as Project[])
		);
	}

	// -------------------------
	// TREE
	// -------------------------

	describeTree(projectId: string): Observable<ProjectNode[]> {

		let params = new HttpParams()
			.set('projectId', projectId);

		return this.http.get<ApiResult>(ProjectService.DescribeTree, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode[])
		);
	}

	listChildren(projectId: string, path: string): Observable<ProjectNode[]> {

		const params = new HttpParams()
			.set('projectId', projectId)
			.set('path', path);

		return this.http.get<ApiResult>(ProjectService.ListChildren, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode[])
		);
	}

	// -------------------------
	// NODE READ
	// -------------------------

	readNode(projectId: string, path: string): Observable<ProjectNode> {

		const params = new HttpParams()
			.set('projectId', projectId)
			.set('path', path);

		return this.http.get<ApiResult>(ProjectService.ReadNode, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode)
		);
	}

	// -------------------------
	// FILE WRITE
	// -------------------------

	writeFile(projectId: string, node: ProjectNode): Observable<ProjectNode> {

		const params = new HttpParams()
			.set('projectId', projectId);

		return this.http.post<ApiResult>(ProjectService.WriteFile, node, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode)
		);
	}

	convertAndAddMarkdown(projectId: string, doc: DocProperties): Observable<string> {
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file, doc.title);

		return this.http.post<ApiResult>(ProjectService.ConvertToMarkdownAndAddFile, formData)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	decomposeMarkdown(projectId: string, doc: DocProperties): Observable<string> {
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file, doc.title);

		return this.http.post<ApiResult>(ProjectService.ConvertToMarkdownDecomposeFile, formData)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	decomposeAndSummarizeMarkdown(projectId: string, doc: DocProperties): Observable<string> {
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file, doc.title);

		return this.http.post<ApiResult>(ProjectService.ConvertToMarkdownDecomposeAndSummarizeFile, formData)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	writeZipFile(projectId: string, file: DocProperties): Observable<string> {
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', file.file, file.title);

		return this.http.post<ApiResult>(ProjectService.ImportZip, formData)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	convertToMermaid(projectId: string, file: DocProperties): Observable<string> {
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', file.file, file.title);

		return this.http.post<ApiResult>(ProjectService.ConvertToMermaid, formData)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	downloadProjectZip(projectId: string) {
		const params = new HttpParams()
			.set('projectId', projectId);

		this.http.get<ApiResult>(ProjectService.ExportZip, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as string)
		).subscribe(base64 => {
			const binary = atob(base64);
			const bytes = new Uint8Array(binary.length);
			for (let i = 0; i < binary.length; i++) {
				bytes[i] = binary.charCodeAt(i);
			}

			const blob = new Blob([bytes], { type: 'application/zip' });
			const url = URL.createObjectURL(blob);
			const link = document.createElement('a');
			link.href = url;
			link.download = 'project-' + projectId + '.zip';
			link.style.display = 'none';
			document.body.appendChild(link);
			link.click();
			document.body.removeChild(link);
			window.URL.revokeObjectURL(url);
		});
	}
	
	// -------------------------
	// CREATE FOLDER
	// -------------------------

	createFolder(projectId: string, path: string): Observable<ProjectNode> {

		const params = new HttpParams()
			.set('projectId', projectId)
			.set('path', path);

		return this.http.post<ApiResult>(ProjectService.CreateFolder, null, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode)
		);
	}

	// -------------------------
	// MOVE NODE
	// -------------------------

	moveNode(projectId: string,
		sourcePath: string,
		targetPath: string): Observable<ProjectNode> {

		const params = new HttpParams()
			.set('projectId', projectId)
			.set('sourcePath', sourcePath)
			.set('targetPath', targetPath);

		return this.http.post<ApiResult>(ProjectService.MoveNode, null,	{ params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as ProjectNode)
		);
	}

	// -------------------------
	// DELETE NODE
	// -------------------------

	deleteNode(projectId: string, path: string): Observable<boolean> {

		const params = new HttpParams()
			.set('projectId', projectId)
			.set('path', path);

		return this.http.delete<ApiResult>(ProjectService.DeleteNode, { params: params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as boolean)
		);
	}

	updateNodeMetadata(projectId: string, oldPath: string, newPath: string, fileType?: string): Observable<boolean> {
		let params = new HttpParams();
		params = params.append('projectId', projectId);
		params = params.append('oldPath', oldPath);
		params = params.append('newPath', newPath);

		if (fileType) {
			params = params.append('fileType', fileType);
		}

		return this.http.post<ApiResult>(ProjectService.UpdateNodeMeta, {}, { params }).pipe(
			catchError(error => {
				this.alertService.postFailure(JSON.stringify(error));
				return EMPTY;
			}),
			map((result: ApiResult) => {
				return result.data as boolean;
			})
		);
	}

	// -------------------------
	// ERROR HANDLER
	// -------------------------

	private handleError() {
		return catchError(error => {
			this.alertService.postFailure(JSON.stringify(error));
			return EMPTY;
		});
	}
}
