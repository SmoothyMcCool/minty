import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { retry } from 'rxjs';
import { AssistantService } from '../../assistant.service';
import { Assistant } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { ConversationComponent } from './conversation.component';
import { ChatMessage } from '../../model/chat-message';
import { UserService } from 'src/app/user.service';
import { DisplayMode, User } from 'src/app/model/user';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';

@Component({
	selector: 'minty-view-conversation',
	imports: [CommonModule, FormsModule, ConversationComponent, ConfirmationDialogComponent],
	templateUrl: 'view-conversation.component.html',
	styleUrls: ['view-assistants.component.css']
})
export class ViewConversationComponent implements OnInit {

	user: User;
	DisplayMode = DisplayMode;

	userText: string = '';
	chatHistory: ChatMessage[] = [];
	waitingForResponse = false;
	queueDepth: number = undefined;

	assistant: Assistant = {
		id: '',
		name: '',
		prompt: '',
		model: '',
		temperature: 0,
		topK: 5,
		ownerId: '',
		shared: false,
		hasMemory: false,
		documentIds: []
	};
	private conversationId: string = '';
	confirmRestartConversationVisible: boolean = false;

	constructor(private route: ActivatedRoute,
		private userService: UserService,
		private conversationService: ConversationService,
		private assistantService: AssistantService) {
	}

	ngOnInit(): void {
		this.userService.getUser().subscribe(user => {
			this.user = user;
			this.route.params.subscribe(params => {
				this.conversationId = params['id'];

				this.assistantService.getAssistantForConversation(this.conversationId).subscribe((assistant: Assistant) => {
					this.assistant = assistant;
					this.conversationService.history(this.conversationId).subscribe((chatHistory: ChatMessage[]) => {
						this.chatHistory = chatHistory;

						// If the last message in the chathistory is from the user, a query is (almost certainly) in progress.
						// Try to resume it.
						if (this.chatHistory && this.chatHistory.length > 0 && this.chatHistory[0].user) {
							this.waitingForResponse = true;
							this.stream(this.conversationId);
						}
					});

				});
			});
		});
	}

	submit(text: string) {
		this.chatHistory.unshift({ user: true, message: text });

		this.assistantService.ask(this.conversationId, this.assistant.id, text).subscribe(streamId => {
			this.waitingForResponse = true;
			this.userText = '';
			setTimeout(() => this.stream(streamId), 0);
		});

	}

	stream(streamId: string) {
		let response = '';
		this.chatHistory.unshift({ user: false, message: response });

		this.assistantService.getStream(streamId).pipe(
			retry({
				delay: 5000
			})
		).subscribe({
			next: (responseChunk) => {
				const notReadyMarker = '~~Not~ready~~';
				if (responseChunk.startsWith(notReadyMarker)) {
					const tasksAhead = parseInt(responseChunk.substring(notReadyMarker.length), 10);
					this.waitingForResponse = true;
					this.queueDepth = tasksAhead > 0 ? tasksAhead : 0;
				} else {
					this.waitingForResponse = false;
					response += responseChunk;
					this.chatHistory[0] = { user: false, message: response };
				}
			},
			error: () => {
				response += '\n\n<strong>Oh no!</strong> An error occurred while streaming the response!\n\n';
			},
			complete: () => {
				if (response !== '') {
					// Only set this to false if we actually received something at some point. Otherwise it just means we completed instantly.
					this.waitingForResponse = false;
				}
			}
		});
	}

	restart() {
		this.conversationService.reset(this.conversationId).subscribe(() => {
			this.chatHistory = [];
		});
	}

	confirmRestartConversation() {
		this.confirmRestartConversationVisible = false;
		this.conversationService.reset(this.conversationId).subscribe(() => {
			this.chatHistory = [];
		});
	}

	restartConversation() {
		this.confirmRestartConversationVisible = true;
	}
}
