import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { ApiResult } from "./model/api-result";
import { catchError, map } from "rxjs/operators";
import { EMPTY, Observable } from "rxjs";
import { AlertService } from "./alert.service";
import { Assistant } from "./model/assistant";
import { UserService } from "./user.service";
import { User } from "./model/user";
import { ChatMessage } from "./model/chat-message";
import { Conversation } from "./model/conversation";

@Injectable({
	providedIn: 'root'
})

export class ConversationService {

	private static readonly NewConversation = 'api/conversation/new';
	private static readonly ListConversations = 'api/conversation/list';
	private static readonly GetConversationHistory = 'api/conversation/history';
	private static readonly DeleteConversation = 'api/conversation/delete';


	constructor(private http: HttpClient, private userService: UserService, private alertService: AlertService) {
	}

	create(assistant: Assistant): Observable<Conversation> {
		const user: User = this.userService.getUser();
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
		const user: User = this.userService.getUser();

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
}