import { HttpClient, HttpHeaders, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { ApiResult } from "./model/api-result";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "./alert.service";
import { Assistant } from "./model/assistant";

@Injectable({
    providedIn: 'root'
})

export class AssistantService {

    private static readonly CreateAssistant = 'api/assistant/new';
    private static readonly ListAssistants = 'api/assistant/list';
    private static readonly AskAssistant = 'api/assistant/ask';
    private static readonly GetAssistant = 'api/assistant/get';
    private static readonly DeleteAssistant = 'api/assistant/delete';
    private static readonly GetAssistantForConversation = '/api/assistant/conversation';

    constructor(private http: HttpClient, private alertService: AlertService) {
    }

    create(assistant: Assistant): Observable<Assistant> {
        const headers: HttpHeaders = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        return this.http.post<ApiResult>(AssistantService.CreateAssistant, assistant, { headers: headers })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Assistant;
                })
            );
    }

    delete(assistant: Assistant): Observable<null> {
        let params: HttpParams = new HttpParams();
        params = params.append('id', assistant.id);

        return this.http.delete<ApiResult>(AssistantService.DeleteAssistant, { params: params})
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return null;
                })
            )
    }

    list(): Observable<Assistant[]> {
        return this.http.get<ApiResult>(AssistantService.ListAssistants)
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Assistant[];
                })
            );
    }

    ask(conversationId: string, assistantId: number, query: string): Observable<string> {
        const headers: HttpHeaders = new HttpHeaders({
            'Content-Type': 'application/json'
        });

        const body = {
            conversationId: conversationId,
            assistantId: assistantId,
            query: query
        };

        return this.http.post<ApiResult>(AssistantService.AskAssistant, body, { headers: headers })
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

    askDefaultAssistant(query: string): Observable<string> {
        return this.ask("default", -1, query);
    }

    getAssistant(id: number): Observable<Assistant> {
        let params: HttpParams = new HttpParams();
        params = params.append('id', id);

        return this.http.get<ApiResult>(AssistantService.GetAssistant, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Assistant;
                })
            );
    }

    getAssistantForConversation(conversationId: string): Observable<Assistant> {
        let params: HttpParams = new HttpParams();
        params = params.append('conversationId', conversationId);

        return this.http.get<ApiResult>(AssistantService.GetAssistantForConversation, { params: params })
            .pipe(
                catchError(error => {
                    this.alertService.postFailure(JSON.stringify(error));
                    return EMPTY;
                }),
                map((result: ApiResult) => {
                    return result.data as Assistant;
                })
            );
    }
}