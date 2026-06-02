import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResult } from './model/api-result';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from './alert.service';
import { MintyDoc } from './model/minty-doc';
import { DocProperties } from './document/document-editor.component';

@Injectable({
	providedIn: 'root'
})

export class DocumentService {

	private static readonly AddDocument = 'api/document/add';
	private static readonly UploadDocument = 'api/document/upload';
	private static readonly ListDocuments = 'api/document/list';
	private static readonly DeleteDocument = 'api/document/delete';

	private static readonly ConvertToMarkdownAndAddFile = 'api/document/convert/markdown';
	private static readonly ConvertToMarkdownDecomposeFile = 'api/document/convert/markdown/decompose';
	private static readonly ConvertToMarkdownDecomposeAndSummarizeFile = 'api/document/convert/markdown/summarize';
	private static readonly ConvertToMermaid = 'api/document/convert/mermaid';

	private static readonly ListTasks = 'api/document/tasks';

	//private mintyDocListSubject: TrackableSubject<MintyDoc[]> = new TrackableSubject<MintyDoc[]>();
	//mintyDocListList$: Observable<MintyDoc[]> = this.mintyDocListSubject.asObservable();

	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	add(document: MintyDoc): Observable<MintyDoc> {
		return this.http.post<ApiResult>(DocumentService.AddDocument, document)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as MintyDoc;
				})
			);
	}

	upload(documentId: string, file: File): Observable<string> {
		const formData = new FormData();
		formData.append('documentId', documentId);
		formData.append('file', file, file.name);

		return this.http.post<ApiResult>(DocumentService.UploadDocument, formData)
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

	list(projectId: string): Observable<MintyDoc[]> {
		let params: HttpParams = new HttpParams();
		params = params.append('projectId', projectId);

		return this.http.get<ApiResult>(DocumentService.ListDocuments, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as MintyDoc[];
				})
			);
	}

	delete(document: MintyDoc): Observable<boolean> {
		let params: HttpParams = new HttpParams();
		params = params.append('documentId', document.id);

		return this.http.delete<ApiResult>(DocumentService.DeleteDocument, { params: params })
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

	convertAndAddMarkdown(projectId: string, doc: DocProperties): Observable<string> {
		if (!doc || !doc.file || !doc.title) {
			throw new Error('Invalid file information.');
		}

		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file!, doc.title);

		return this.http.post<ApiResult>(DocumentService.ConvertToMarkdownAndAddFile, formData)
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
		if (!doc || !doc.file || !doc.title) {
			throw new Error('Invalid file information.');
		}

		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file!, doc.title);

		return this.http.post<ApiResult>(DocumentService.ConvertToMarkdownDecomposeFile, formData)
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
		if (!doc || !doc.file || !doc.title) {
			throw new Error('Invalid file information.');
		}

		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', doc.file, doc.title);

		return this.http.post<ApiResult>(DocumentService.ConvertToMarkdownDecomposeAndSummarizeFile, formData)
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
		if (!file || !file.file || !file.title) {
			throw new Error('Invalid file information.');
		}
		const formData = new FormData();
		formData.append('projectId', projectId);
		formData.append('file', file.file, file.title);

		return this.http.post<ApiResult>(DocumentService.ConvertToMermaid, formData)
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

	listTasks(): Observable<string[]> {

		return this.http.get<ApiResult>(DocumentService.ListTasks).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as string[])
		);
	}

	// -------------------------
	// ERROR HANDLER
	// -------------------------

	private handleError<T>() {
		return catchError<T, Observable<never>>(error => {
			this.alertService.postFailure(JSON.stringify(error));
			return EMPTY;
		});
	}

}