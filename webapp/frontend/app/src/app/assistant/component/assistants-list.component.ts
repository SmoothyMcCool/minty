import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { ConversationService } from '../../conversation.service';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserService } from 'src/app/user.service';
import { Conversation } from 'src/app/model/conversation';
import { FilterPipe } from 'src/app/pipe/filter-pipe';
import { User } from 'src/app/model/user';

@Component({
	selector: 'minty-assistants-list',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, ConfirmationDialogComponent],
	templateUrl: 'assistants-list.component.html'
})
export class AssistantsListComponent implements OnInit {

	conversations: Conversation[] = [];
	selectedChat: string = '';

	assistants: Assistant[] = [];
	sharedAssistants: Assistant[] = [];

	fileList: File[] = [];
	workingAssistant: Assistant = {
		id: '',
		name: '',
		prompt: '',
		model: '',
		temperature: 0,
		numFiles: 0,
		ownerId: '',
		shared: false,
		hasMemory: false,
		documentIds: []
	};

	deleteInProgress = false;

	confirmDeleteAssistantVisible = false;
	assistantPendingDeletion: Assistant;
	confirmDeleteConversationVisible = false;
	conversationPendingDeletionId: string;

	conversationToRename: Conversation = null;
	renamedConversationTitle: string;
	renameConversationVisible = false;

	user: User;

	constructor(
		private assistantService: AssistantService,
		private conversationService: ConversationService,
		private userService: UserService,
		private router: Router) {

		this.assistantService.list().subscribe(assistants => {
			setTimeout(() => {
				this.assistants = assistants;
				this.sharedAssistants = assistants.filter(assistant => assistant.shared === true);
			}, 0);
		});

		this.conversationService.list().subscribe(conversations => {
			this.conversations = conversations;
		});
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
	}

	editAssistant(assistantId: number) {
		this.router.navigate(['/assistants/edit', assistantId]);
	}

	deleteAssistant(assistant: Assistant) {
		this.confirmDeleteAssistantVisible = true;
		this.assistantPendingDeletion = assistant;
	}

	confirmDeleteAssistant() {
		this.deleteInProgress = true;
		this.confirmDeleteAssistantVisible = false;
		this.assistantService.delete(this.assistantPendingDeletion).subscribe(() => {
			this.assistantService.list().subscribe(assistants => {
				this.assistants = assistants;
				this.sharedAssistants = assistants.filter(assistant => assistant.shared === true);
				this.deleteInProgress = false;
			});
			this.conversationService.list().subscribe(conversations => {
				this.conversations = conversations;
			});
		});
		this.assistants = this.assistants.filter(item => item.id === this.assistantPendingDeletion.id);
		this.sharedAssistants = this.assistants.filter(assistant => assistant.shared === true);
	}

	startConversation(assistant: Assistant): void {
		this.conversationService.create(assistant).subscribe( conversation => {
			this.router.navigate(['/conversation', conversation.conversationId]);
		});
	}

	selectConversation(conversation: Conversation) {
		this.router.navigate(['/conversation', conversation.conversationId]);
	}

	deleteConversation(conversation: Conversation) {
		this.confirmDeleteConversationVisible = true;
		this.conversationPendingDeletionId = conversation.conversationId;
	}

	confirmDeleteConversation() {
		this.confirmDeleteConversationVisible = false;
		this.conversationService.delete(this.conversationPendingDeletionId).subscribe(() => {
			this.assistantService.list().subscribe(assistants => {
				this.assistants = assistants;
			});
			this.conversationService.list().subscribe(conversations => {
				this.conversations = conversations;
			});
		});
		this.conversations = this.conversations.filter(item => item.conversationId === this.conversationPendingDeletionId);
	}

	fileListChanged(event: Event) {
		const newFiles = (event.target as HTMLInputElement).files;
		if (newFiles) {
			this.fileList = Array.from(newFiles).concat(Array.from(this.fileList));
			this.fileList = [...new Set(this.fileList)];
		}
	}

	removeFile(filename: string) {
		this.fileList = this.fileList.filter(element => element.name != filename);
	}

	isOwned(assistant: Assistant): boolean {
		return assistant.ownerId === this.user.id;
	}

	getConversationTitle(conversation: Conversation): string {
		if (conversation.title !== null && conversation.title.length > 0) {
			return conversation.title;
		}
		return conversation.conversationId;
	}

	getConversationAssistantName(conversation: Conversation): string {
		const assistant = this.assistants.find( assistant => assistant.id === conversation.associatedAssistantId);
		if (assistant) {
			return assistant.name;
		}
		return '';
	}

	renameConversation(conversation: Conversation) {
		this.renameConversationVisible = true;
		this.conversationToRename = conversation;
		this.renamedConversationTitle = conversation.title;
	}

	onConfirmConversationRename() {
		this.conversationToRename.title = this.renamedConversationTitle;
		this.renameConversationVisible = false;
		this.conversationService.rename(this.conversationToRename).subscribe(conversation => {
		});
	}

	onCancelConversationRename() {
		this.renameConversationVisible = false;
	}
}
