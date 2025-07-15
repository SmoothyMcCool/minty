import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { ApiResult } from "./model/api-result";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "./alert.service";
import { UserMeta } from "./model/user-meta";

@Injectable({
    providedIn: 'root'
})
export class MetadataService {

    private static readonly GetMetadata = 'api/metadata/all';

    constructor(private http: HttpClient,
        private alertService: AlertService) {
    }

    getMetadata(): Observable<UserMeta[]> {
        return this.http.get<ApiResult>(MetadataService.GetMetadata)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    let results =  result.data as UserMeta[];
                    results.forEach((result: UserMeta) => {
                        result.lastLogin = new Date(result.lastLogin);
                    });
                    return results;
                })
            );
    }

}