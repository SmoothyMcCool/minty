import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { retry } from 'rxjs';
import { AssistantService } from '../../assistant.service';
import { Assistant, createAssistant } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { ConversationComponent } from './conversation.component';
import { ChatMessage } from '../../model/conversation/chat-message';
import { UserService } from 'src/app/user.service';
import { User } from 'src/app/model/user';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { Conversation } from 'src/app/model/conversation/conversation';
import { ImageInputComponent } from './image-input.component';
import { LlmMetric } from 'src/app/model/conversation/llm-metric';
import { StreamingResponse } from 'src/app/model/conversation/streaming-response';
import { Model } from 'src/app/model/model';
import { SliderComponent } from './slider.component';

@Component({
	selector: 'minty-view-conversation',
	imports: [CommonModule, FormsModule, ConversationComponent, ConfirmationDialogComponent, ImageInputComponent, SliderComponent],
	templateUrl: 'view-conversation.component.html'
})
export class ViewConversationComponent implements OnInit, OnDestroy {

	user: User;

	userText: string = '';
	chatHistory: ChatMessage[] = [];
	waitingForResponse = false;
	queueDepth: number = undefined;
	image: File = undefined;
	shouldReset: boolean = false;

	assistant: Assistant = createAssistant();
	private conversationId: string = '';
	conversation: Conversation = null;
	metrics: LlmMetric;
	sources: Set<string>;
	model: Model;
	contextSize: number;

	private conversationTimeoutId: NodeJS.Timeout;
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

					this.assistantService.models().subscribe(models => {
						this.model = models.find(model => model.name.localeCompare(this.assistant.model) === 0);
						this.contextSize = this.model.defaultContext;
					});

					if (this.assistant.hasMemory) {
						this.conversationService.history(this.conversationId).subscribe((chatHistory: ChatMessage[]) => {
							this.chatHistory = chatHistory;

							// If the last message in the chathistory is from the user, a query is (almost certainly) in progress.
							// Try to resume it.
							if (this.chatHistory && this.chatHistory.length > 0 && this.chatHistory[0].user) {
								this.waitingForResponse = true;
								this.stream(this.conversationId);
							}
						});
						this.pollConversation();
					}

				});
			});
		});
	}

	pollConversation() {
		if (!this.conversation?.title) {
			this.conversationService.getConversation(this.conversationId).subscribe((conversation: Conversation) => {
				if (conversation?.title) {
					this.conversation = conversation;
					return;
				}
				this.conversationTimeoutId = setTimeout(() => this.pollConversation(), 5000);
			});
		}
	}

	ngOnDestroy(): void {
		clearTimeout(this.conversationTimeoutId);
	}

	submit(text: string) {
		this.chatHistory.unshift({ user: true, message: text });

		this.assistantService.ask(this.conversationId, this.assistant.id, text, this.image, this.contextSize).subscribe(streamId => {
			this.waitingForResponse = true;
			this.userText = '';
			this.shouldReset = true;
			setTimeout(() => {
				this.stream(streamId);
				this.shouldReset = false;
			}, 0);
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
			next: (responseChunk: StreamingResponse) => {

				if (responseChunk.status.state == 'NOT_READY') {
					this.waitingForResponse = true;
					this.queueDepth = responseChunk.status.queuePosition > 0 ? responseChunk.status.queuePosition : 0;
				} else {
					if (responseChunk.metric) {
						this.metrics = responseChunk.metric;
					}
					if (responseChunk.sources) {
						if (!this.sources) {
							this.sources = new Set<string>();
						}
						responseChunk.sources.forEach(source => this.sources.add(source));
					}
					if (responseChunk.content) {
						response += responseChunk.content;
					}
					if (response.length > 0) {
						this.waitingForResponse = false;
					}
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

	onImageChanged(image: File) {
		this.image = image;
	}
}
