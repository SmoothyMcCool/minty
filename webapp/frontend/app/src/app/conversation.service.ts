import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResult } from './model/api-result';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from './alert.service';
import { Assistant } from './model/assistant';
import { ChatMessage } from './model/conversation/chat-message';
import { Conversation } from './model/conversation/conversation';

@Injectable({
	providedIn: 'root'
})

export class ConversationService {

	private static readonly NewConversation = 'api/conversation/new';
	private static readonly ListConversations = 'api/conversation/list';
	private static readonly GetConversation = 'api/conversation';
	private static readonly GetConversationHistory = 'api/conversation/history';
	private static readonly DeleteConversation = 'api/conversation/delete';
	private static readonly ResetConversation = 'api/conversation/reset';
	private static readonly RenameConversation = 'api/conversation/rename';

	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	create(assistant: Assistant): Observable<Conversation> {
		let params: HttpParams = new HttpParams();
		params = params.append('assistantId', assistant.id);

		return this.http.get<ApiResult>(ConversationService.NewConversation, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Conversation;
				})
			);
	}

	list(): Observable<Conversation[]> {
		return this.http.get<ApiResult>(ConversationService.ListConversations)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Conversation[];
				})
			);
	}

	getConversation(conversationId: string) : Observable<Conversation> {
		let params: HttpParams = new HttpParams();
		params = params.append('conversationId', conversationId);

		return this.http.get<ApiResult>(ConversationService.GetConversation, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Conversation;
				})
			);
	}

	delete(conversationId: string): Observable<string> {
		let params: HttpParams = new HttpParams();
		params = params.append('conversationId', conversationId);

		return this.http.delete<ApiResult>(ConversationService.DeleteConversation, { params: params })
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

	reset(conversationId: string): Observable<string> {
		let params: HttpParams = new HttpParams();
		params = params.append('conversationId', conversationId);

		return this.http.delete<ApiResult>(ConversationService.ResetConversation, { params: params })
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

	history(conversationId: string): Observable<ChatMessage[]> {
		let params: HttpParams = new HttpParams();
		params = params.append('conversationId', conversationId);

		return this.http.get<ApiResult>(ConversationService.GetConversationHistory, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as ChatMessage[];
				})
			);
	}

	rename(conversation: Conversation) {
		let params: HttpParams = new HttpParams();
		params = params.append('conversationId', conversation.conversationId);
		params = params.append('title', conversation.title);

		return this.http.get<ApiResult>(ConversationService.RenameConversation, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as Conversation;
				})
			);
	}
}