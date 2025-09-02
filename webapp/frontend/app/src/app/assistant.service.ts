import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResult } from './model/api-result';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from './alert.service';
import { Assistant } from './model/assistant';

@Injectable({
	providedIn: 'root'
})

export class AssistantService {

	private static readonly CreateAssistant = 'api/assistant/new';
	private static readonly EditAssistant = 'api/assistant/edit';
	private static readonly ListAssistants = 'api/assistant/list';
	private static readonly AskAssistant = 'api/assistant/ask';
	private static readonly GetAssistant = 'api/assistant/get';
	private static readonly DeleteAssistant = 'api/assistant/delete';
	private static readonly GetAssistantForConversation = 'api/assistant/conversation';
	private static readonly ListModels = 'api/assistant/models';

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

	update(assistant: Assistant) {
		const headers: HttpHeaders = new HttpHeaders({
			'Content-Type': 'application/json'
		});

		return this.http.post<ApiResult>(AssistantService.EditAssistant, assistant, { headers: headers })
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
				map((_result: ApiResult) => {
					return null;
				})
			);
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

	models(): Observable<string[]> {
		return this.http.get<ApiResult>(AssistantService.ListModels)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string[];
				})
			);
	}

	ask(conversationId: string, assistantId: string, query: string): Observable<string> {
		const body = {
			conversationId: conversationId,
			assistantId: assistantId,
			query: query
		};

		// Hideous, but it works?!?!?!
		return new Observable(observer => {
			fetch(AssistantService.AskAssistant, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-Requested-With': 'XMLHttpRequest',
					'x-auth-token': sessionStorage.getItem('x-auth-token')
				},
				body: JSON.stringify(body)

			}).then(response => {
				const reader = response.body?.getReader();
				const decoder = new TextDecoder();

				function read() {
					reader?.read().then(({ done, value }) => {
					if (done) {
						observer.complete();
						return;
					}

					const chunk = decoder.decode(value, { stream: true });
					observer.next(chunk);
					read();
					});
				}

				read();
				}).catch(err => observer.error(err));
		});
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