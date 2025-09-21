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
	private static readonly GetResponseStream = 'api/assistant/response';
	private static readonly GetAssistant = 'api/assistant/get';
	private static readonly DeleteAssistant = 'api/assistant/delete';
	private static readonly GetAssistantForConversation = 'api/assistant/conversation';
	private static readonly ListModels = 'api/assistant/models';
	private static readonly GetConversationQueryId = 'api/assistant/queryId';

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

		return this.http.delete<ApiResult>(AssistantService.DeleteAssistant, { params: params })
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

	queryId(conversationId: string, assistantId: string): Observable<string> {
		const body = {
			conversationId: conversationId,
			assistantId: assistantId,
			query: null
		};

		return this.http.post<ApiResult>(AssistantService.GetConversationQueryId, body)
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

	ask(conversationId: string, assistantId: string, query: string): Observable<string> {
		const body = {
			conversationId: conversationId,
			assistantId: assistantId,
			query: query
		};

		return this.http.post<ApiResult>(AssistantService.AskAssistant, body)
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

	getStream(streamId: string): Observable<string> {

		// Hideous, but it works?!?!?!
		return new Observable<string>(observer => {
			fetch(AssistantService.GetResponseStream, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-Requested-With': 'XMLHttpRequest',
					'x-auth-token': sessionStorage.getItem('x-auth-token')
				},
				body: JSON.stringify(streamId)

			}).then(response => {
				const reader = response.body?.getReader();
				const decoder = new TextDecoder();

				async function read() {
					try {
						const { done, value } = await reader.read();

						if (done) {
							observer.complete();
							return;
						}

						const chunk = decoder.decode(value, { stream: true });
						const notReadyMarker = '~~Not~ready~~';
						if (chunk.startsWith(notReadyMarker)) {
							observer.next(chunk);
							observer.error(new Error('Not ready'));
							return;
						}
						observer.next(chunk);
						read();
					} catch (error) {
						console.log('Error readyin stream: ', error);
						observer.error(error);
					}
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