import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResult } from './model/api-result';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from './alert.service';
import { Assistant } from './model/assistant';
import { AssistantSpec } from './model/workflow/assistant-spec';
import { Model } from './model/model';
import { StreamingResponse } from './model/conversation/streaming-response';

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
	private static readonly GetDiagrammingAssistant = 'api/assistant/diagram';

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

	models(): Observable<Model[]> {
		return this.http.get<ApiResult>(AssistantService.ListModels)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Model[];
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

	ask(conversationId: string, assistantId: string, query: string, image: File, contextSize: number): Observable<string> {
		
		const form = new FormData();
		form.append('conversationId', conversationId);
		form.append('query', query);
		form.append('contextSize', '' + contextSize);
		if (image) {
			form.append('image', image);
		}

		const assistantSpec: AssistantSpec = {
			assistantId: assistantId,
			assistant: null
		};
		form.append('assistant', new Blob([JSON.stringify(assistantSpec)], { type: "application/json" }));
		

		return this.http.post<ApiResult>(AssistantService.AskAssistant, form)
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

	getStream(streamId: string): Observable<StreamingResponse> {

		// Hideous, but it works?!?!?!
		return new Observable<StreamingResponse>(observer => {
			fetch(AssistantService.GetResponseStream, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-Requested-With': 'XMLHttpRequest',
					'x-auth-token': sessionStorage.getItem('x-auth-token')
				},
				body: JSON.stringify(streamId)

			}).then(response => {
				const reader = response.body!.getReader();
				const streamState = {
					buffer: '',
					decoder: new TextDecoder()
				};

				(async () => {
					try {
						while (true) {
							const messages = await this.readJsonStream(reader, streamState);

							if (!messages) {
								observer.complete();
								return;
							}

							for (const message of messages) {
								observer.next(message);
							}
						}

					} catch (error) {
						console.log('Error reading stream: ', error);
						observer.error(error);
					}
				})();

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

	getDiagrammingAssistant() {
		return this.http.get<ApiResult>(AssistantService.GetDiagrammingAssistant)
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

	private async readJsonStream(reader: ReadableStreamDefaultReader<Uint8Array>, state: { buffer: string, decoder: TextDecoder }): Promise<StreamingResponse[] | null> {

		const { done, value } = await reader.read();
		if (done) {
			return null;
		}

		state.buffer += state.decoder.decode(value, { stream: true });

		const results: StreamingResponse[] = [];
		const lines = state.buffer.split('\n');

		// Keep the incomplete JSON fragment
		state.buffer = lines.pop() ?? '';

		for (const line of lines) {
			const trimmed = line.trim();
			if (!trimmed) {
				continue;
			}

			try {
				results.push(JSON.parse(trimmed) as StreamingResponse);
			} catch {
				throw new Error(`Invalid JSON in stream: ${trimmed}`);
			}
		}

		return results;
	}
}