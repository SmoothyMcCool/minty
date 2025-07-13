import { HttpClient, HttpHeaders } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { ApiResult } from "./model/api-result";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "./alert.service";

@Injectable({
    providedIn: 'root'
})

export class DocumentService {

    private static readonly AddDocument = 'api/document/add';

    constructor(private http: HttpClient, private alertService: AlertService) { }

    upload(assistantId: number, file: File): Observable<string> {
        const formData = new FormData();
        formData.append("assistantId", assistantId.toString());
        formData.append("file", file, file.name);

        return this.http.post<ApiResult>(DocumentService.AddDocument, formData)
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
}