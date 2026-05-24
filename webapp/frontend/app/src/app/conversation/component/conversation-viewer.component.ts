import { Component, ElementRef, forwardRef, Input, OnDestroy, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { retry } from 'rxjs';
import { AssistantService } from '../../assistant.service';
import { AgentStepResult, Assistant, createAssistant } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { ChatMessage } from '../../model/conversation/chat-message';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { Conversation } from '../../model/conversation/conversation';
import { LlmMetric } from '../../model/conversation/llm-metric';
import { StreamingResponse } from '../../model/conversation/streaming-response';
import { Model } from '../../model/model';
import { User } from '../../model/user';
import { AutoResizeDirective } from '../../pipe/auto-resize-directive';
import { UserService } from '../../user.service';
import { ProjectService } from '../../project/project.service';
import { ProjectNode } from '../../model/project/project-node';
import { NodeViewerComponent } from '../../project/component/project-node-viewer.component';
import { ConversationComponent } from './conversation.component';
import { ImageInputComponent } from './image-input.component';
import { SliderComponent } from './slider.component';


@Component({
	selector: 'minty-conversation-viewer',
	imports: [CommonModule, FormsModule, ConversationComponent, ConfirmationDialogComponent, ImageInputComponent, SliderComponent, AutoResizeDirective, NodeViewerComponent],
	templateUrl: 'conversation-viewer.component.html',
	styleUrl: 'conversation-viewer.component.css',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => ConversationViewerComponent),
			multi: true
		}
	]
})
export class ConversationViewerComponent implements ControlValueAccessor, OnDestroy {

	conversation: Conversation | undefined = undefined;

	@ViewChild('aiQueryEl') aiQueryEl!: ElementRef<HTMLTextAreaElement>;

	onChange = (_: any) => { };
	onTouched: any = () => { };

	user!: User;

	userText: string = '';
	chatHistory: ChatMessage[] = [];
	waitingForResponse = false;
	responseComplete = true;
	queueDepth: number | undefined = undefined;
	image: File | undefined = undefined;
	shouldReset: boolean = false;

	files: string[] | null = [];
	activeProject: string | undefined = undefined;
	fileNode: ProjectNode | undefined = undefined;
	showFilesColumn: boolean = true;

	assistant: Assistant = createAssistant();
	showChatOptions = false;
	newestMessagesFirst = true;
	reverseButtons = false;
	metrics: LlmMetric | undefined = undefined;
	sources: Set<string> | undefined = undefined;
	statusMessages: AgentStepResult[] = [];
	expandedSteps: Record<number, boolean> = {};
	model: Model | undefined = undefined;
	contextSize: number = 16384;

	private conversationTimeoutId: NodeJS.Timeout | undefined = undefined;
	confirmRestartConversationVisible: boolean = false;

	constructor(private userService: UserService,
		private conversationService: ConversationService,
		private assistantService: AssistantService,
		private projectService: ProjectService) {
	}

	initialize(): void {
		this.userService.getUser().subscribe(user => {

			this.user = user;
			this.newestMessagesFirst = user.settings['Message Order'] ? user.settings['Message Order'] == 'NewestFirst' : false;
			this.reverseButtons = user.settings['Button Alignment'] ? user.settings['Button Alignment'] == 'Right' : false;

			this.activeProject = user.defaults['defaultProject'];

			this.assistantService.getAssistantForConversation(this.conversation?.id!).subscribe((assistant: Assistant) => {

				this.assistant = assistant;
				this.contextSize = this.assistant.contextSize;

				this.assistantService.models().subscribe(models => {
					this.model = models.find(model => model.name.localeCompare(this.assistant.model) === 0);
				});

				if (this.assistant.hasMemory) {
					this.conversationService.history(this.conversation!.id).subscribe((chatHistory: ChatMessage[]) => {
						this.chatHistory = chatHistory;
						chatHistory.forEach(message => {
							if (!message.user) {
								this.addFiles(this.assistantService.getFileListFromMessage(message.message));
							}
						})

						// If the last message in the chathistory is from the user, a query is (almost certainly) in progress.
						// Try to resume it.
						if (this.chatHistory && this.chatHistory.length > 0 && this.chatHistory[0].user) {
							this.waitingForResponse = true;
							this.stream(this.conversation!.id);
						}
					});
					this.pollConversation();
				}

			});
		});
	}

	pollConversation() {
		if (!this.conversation?.title) {
			this.conversationService.getConversation(this.conversation!.id).subscribe((conversation: Conversation) => {
				if (conversation?.title) {
					this.conversation = conversation;
					this.onTouched();
					this.onChange(this.conversation);
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

		this.assistantService.ask(this.conversation!.id, this.assistant.id, text, this.image ?? null, this.contextSize).subscribe(streamId => {
			this.waitingForResponse = true;
			this.responseComplete = false;

			this.userText = '';
			setTimeout(() => {
				const el = this.aiQueryEl.nativeElement;
				el.style.height = 'auto';
				if (!el.value.trim()) {
					el.style.height = window.getComputedStyle(el).minHeight;
				}
				el.style.height = `${el.scrollHeight}px`;
			});

			this.shouldReset = true;
			this.statusMessages = [];
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
						responseChunk.sources.forEach(source => this.sources!.add(source));
					}
					if (responseChunk.content) {
						if (responseChunk.content.startsWith('[STATUS]')) {
							const message = responseChunk.content.substring('[STATUS]'.length).replace(/\\n/g, '\n').trim() + '\n';
							this.statusMessages.push({ statusMessage: message, stepOutput: '' });
							console.log('raw message:', message);

						} else if (responseChunk.content.startsWith('[INTERNAL]')) {
							let message = responseChunk.content.substring('[INTERNAL]'.length).replace(/\\n/g, '\n').trim() + '\n';

							const start = message.indexOf("[") + 1;
							const end = message.indexOf("]");
							const stepName = message.substring(start, end);

							message = message.substring(end + 1);

							const statusStep = this.statusMessages.find(message => message.statusMessage?.includes(stepName));
							if (statusStep) {
								statusStep.stepOutput += message;
							}
							this.statusMessages = [...this.statusMessages];
							console.log('raw message:', message);

						} else {
							response += responseChunk.content;
						}
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
				this.waitingForResponse = false;
				this.responseComplete = true;
				if (response == '') {
					this.chatHistory[0] = { user: false, message: '<em>No response from server. Your request likely failed.</em>' };
				}
				// Check to see if the response includes a list of files to display.
				this.addFiles(this.assistantService.getFileListFromMessage(response));
				if (this.files) {
					console.log(JSON.stringify(this.files));
				}
			}
		});
	}

	addFiles(files: string[] | null) {
		if (!files) {
			return;
		}
		this.files = [...new Set([...this.files!, ...files])];
	}

	onFileClick(file: string) {
		if (this.activeProject && file) {
			this.projectService.readNode(this.activeProject, file).subscribe(node => {
				this.fileNode = node;
			});
		}
	}

	restart() {
		if (!this.waitingForResponse && this.responseComplete) { // Streaming in progress
			this.cancelStream();
		}
		this.conversationService.reset(this.conversation!.id).subscribe(() => {
			this.chatHistory = [];
		});
	}

	confirmRestartConversation() {
		this.confirmRestartConversationVisible = false;
		this.conversationService.reset(this.conversation!.id).subscribe(() => {
			this.chatHistory = [];
			this.statusMessages = [];
			this.files = [];
		});
	}

	restartConversation() {
		this.confirmRestartConversationVisible = true;
	}

	cancelStream() {
		this.assistantService.cancelStream(this.conversation!.id).subscribe(() => {
			this.statusMessages = [];
		});
	}

	onImageChanged(image: File) {
		this.image = image;
	}

	trackByIndex(index: number) {
		return index;
	}

	toggleStep(i: number) {
		this.expandedSteps[i] = !this.expandedSteps[i];
	}

	writeValue(obj: any): void {
		if (obj == null) {
			return;
		}
		this.conversation = { ...obj as Conversation }
		this.initialize();
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Nah.
	}
}
