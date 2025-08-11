import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { ApiResult } from "./model/api-result";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "./alert.service";
import { MintyDoc } from "./model/minty-doc";

@Injectable({
	providedIn: 'root'
})

export class DocumentService {

	private static readonly AddDocument = 'api/document/add';
	private static readonly UploadDocument = 'api/document/upload';
	private static readonly ListDocuments = 'api/document/list';
	private static readonly DeleteDocument = 'api/document/delete';

	constructor(private http: HttpClient, private alertService: AlertService) { }

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

	list(): Observable<MintyDoc[]> {
		return this.http.get<ApiResult>(DocumentService.ListDocuments)
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
		params = params.append('documentId', document.documentId);

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
}