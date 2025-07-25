import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "../alert.service";
import { ApiResult } from "../model/api-result";
import { TrackableSubject } from "../trackable-subject";

@Injectable({
    providedIn: 'root'
})
export class TaskTemplateService {

    private resultListSubject: TrackableSubject<string[]> = new TrackableSubject<string[]>();
    resultList$: Observable<string[]> = this.resultListSubject.asObservable();

    private static readonly ListTemplates = 'api/task/template/list'; // List all templates that can be used to create tasks
    private static readonly GetTemplateConfiguration = 'api/task/template/config'; // Get description of the configuration of a task template
    private static readonly ListTemplateConfigurations = 'api/task/template/config/list'; // Get description of the configuration of all task templates

    private static readonly ListOutputTemplates = 'api/task/template/output/list'; // List all output tasks that can be used to format task output
    private static readonly GetOutputTemplatesConfiguration = 'api/task/template/output/config'; // Get description of the configuration of an output template
    private static readonly ListOutputTemplateConfigurations = 'api/task/template/output/config/list'; // Get description of the configuration of all output templates

    constructor(private http: HttpClient, private alertService: AlertService) {
    }

    listTemplates(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(TaskTemplateService.ListTemplates)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const map = new Map<string, Map<string, string>>(Object.entries(result.data as any));
                    const resultMap = new Map<string, Map<string, string>>();

                    map.forEach((value, key, map) => {
                        resultMap.set(key, new Map<string, string>(Object.entries(value)));
                    });

                    return resultMap;
                })
            );
    }

    getTemplateConfiguration(taskId: number): Observable<Map<string, string>> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskTemplateService.GetTemplateConfiguration, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return new Map<string, string>(Object.entries(result.data as any));
                })
            );
    }

    listAllTemplateConfigurations(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(TaskTemplateService.ListTemplateConfigurations)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const ret = new Map<string, Map<string, string>>(Object.entries(result.data));
                    ret.forEach((value, key) => {
                        ret.set(key, new Map<string, string>(Object.entries(value)));
                    })
                    return ret;
                })
            );
    }

    getOutputTemplateConfiguration(taskId: number): Observable<Map<string, string>> {
        let params: HttpParams = new HttpParams();
        params = params.append('taskId', taskId);

        return this.http.get<ApiResult>(TaskTemplateService.GetOutputTemplatesConfiguration, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return new Map<string, string>(Object.entries(result.data as any));
                })
            );
    }

    listAllOutputTemplateConfigurations(): Observable<Map<string, Map<string, string>>> {
        return this.http.get<ApiResult>(TaskTemplateService.ListOutputTemplateConfigurations)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    const ret = new Map<string, Map<string, string>>(Object.entries(result.data));
                    ret.forEach((value, key) => {
                        ret.set(key, new Map<string, string>(Object.entries(value)));
                    })
                    return ret;
                })
            );
    }

    listOutputTemplates(): Observable<Map<string, Map<string, string>>> {
    return this.http.get<ApiResult>(TaskTemplateService.ListOutputTemplates)
        .pipe(
            catchError(error => {
                this.alertService.postFailure(JSON.stringify(error));
                return EMPTY;
            }),
            map((result: ApiResult) => {
                const map = new Map<string, Map<string, string>>(Object.entries(result.data as any));
                const resultMap = new Map<string, Map<string, string>>();

                map.forEach((value, key, map) => {
                    resultMap.set(key, new Map<string, string>(Object.entries(value)));
                });

                return resultMap;
            })
        );
    }

}