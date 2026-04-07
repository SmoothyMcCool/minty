import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, createAssistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { ConversationService } from '../../conversation.service';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { UserSelectDialogComponent, UserSelection } from '../../app/component/user-select-dialog.component';
import { Conversation } from '../../model/conversation/conversation';
import { User } from '../../model/user';
import { FilterPipe } from '../../pipe/filter-pipe';
import { PredicatePipe } from '../../pipe/predicate-pipe';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-assistants-list',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, ConfirmationDialogComponent, PredicatePipe, UserSelectDialogComponent],
	templateUrl: 'assistants-list.component.html'
})
export class AssistantsListComponent implements OnInit {

	conversations: Conversation[] = [];
	selectedChat: string = '';

	assistants: Assistant[] = [];
	sharedAssistants: Assistant[] = [];
	assistantFilter: string = '';
	conversationSortOrder: string = 'lastUsed';

	fileList: File[] = [];
	workingAssistant: Assistant = createAssistant();

	deleteInProgress = false;

	confirmDeleteAssistantVisible = false;
	assistantPendingDeletion!: Assistant;
	confirmDeleteConversationVisible = false;
	conversationPendingDeletionId: string  | undefined = undefined;

	conversationToRename: Conversation | undefined = undefined;
	renamedConversationTitle: string  | undefined = undefined;
	renameConversationVisible = false;

	userSelectDialogVisible = false;
	assistantToShare: string | undefined = undefined;
	sharingSelection: UserSelection | undefined = undefined;

	onlyOwnedAssistants = false;

	user!: User;

	constructor(
		private assistantService: AssistantService,
		private conversationService: ConversationService,
		private alertService: AlertService,
		private userService: UserService,
		private router: Router) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;

			this.assistantService.list().subscribe(assistants => {
				setTimeout(() => {
					this.sortAssistants(assistants);
					this.assistants = assistants;
					this.sharedAssistants = assistants.filter(assistant => assistant.owned === false);
				}, 0);
			});

			this.conversationService.list().subscribe(conversations => {
				this.sortConversations(conversations);
				this.conversations = conversations;
			});

		});
	}

	filteredAssistants() {
		return this.assistants.filter(a => {
			if (this.onlyOwnedAssistants && !a.owned) {
				return false;
			}

			const text = (this.assistantFilter ?? '').toString().trim().toLowerCase();
			if (!text) {
				return true;
			}
			const name = (a.name ?? '').toString().toLowerCase();
			return name.includes(text);
		});
	}

	sortConversationsBy(ordering: string) {
		this.conversationSortOrder = ordering;
		this.sortConversations(this.conversations);
	}

	sortConversations(conversations: Conversation[]) {
		conversations.sort((left, right) => {
			if (this.conversationSortOrder === 'alpha') {
				if (!left.title && !right.title) {
					return left.conversationId.localeCompare(right.conversationId);
				}
				if (!left.title) {
					return 1;
				}
				if (!right.title) {
					return -1;
				}
				return left.title.localeCompare(right.title);
			} else {
				if (!left.lastUsed && ! right.lastUsed) {
					return left.conversationId.localeCompare(right.conversationId);
				}
				if (!left.lastUsed) {
					return 1;
				}
				if (!right.lastUsed) {
					return -1;
				}
				return right.lastUsed - left.lastUsed;
			}
		});
	}

	sortAssistants(assistants: Assistant[]) {
		assistants.sort((left, right) => {
			if (!left.name && !right.name) {
				return left.id.localeCompare(right.id);
			}
			if (!left.name) {
				return 1;
			}
			if (!right.name) {
				return -1;
			}
			return left.name.localeCompare(right.name);
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
				this.sortAssistants(this.assistants);
				this.sharedAssistants = this.assistants.filter(assistant => assistant.owned === false);
				this.deleteInProgress = false;
			});
			this.conversationService.list().subscribe(conversations => {
				this.conversations = conversations;
				this.sortConversations(this.conversations);
			});
		});

		this.sortAssistants(this.assistants);
		this.assistants = this.assistants.filter(item => item.id === this.assistantPendingDeletion.id);
		this.sharedAssistants = this.assistants.filter(assistant => assistant.owned === false);

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
		this.conversationService.delete(this.conversationPendingDeletionId!).subscribe(() => {
			this.assistantService.list().subscribe(assistants => {
				this.sortAssistants(assistants);
				this.assistants = assistants;
				this.sharedAssistants = this.assistants.filter(assistant => assistant.owned === false);
			});
			this.conversationService.list().subscribe(conversations => {
				this.conversations = conversations;
				this.sortConversations(this.conversations);
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
		return assistant.owned;
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
		if (this.conversationToRename) {
			this.conversationToRename.title = this.renamedConversationTitle!;
			this.renameConversationVisible = false;
			this.conversationService.rename(this.conversationToRename!).subscribe(conversation => {
			});
		} else {
			console.log('onConfirmConversationRename: no conversation to rename set');
		}
	}

	onCancelConversationRename() {
		this.renameConversationVisible = false;
	}

	shareAssistant(assistant: Assistant) {
		this.userSelectDialogVisible = true;
		this.assistantToShare = assistant.id
		this.assistantService.getSharingList(assistant.id).subscribe(userSelection => {
			this.sharingSelection = userSelection;
		});
	}

	onUsersConfirmed(selection: UserSelection): void {
		this.userSelectDialogVisible = false;
		this.assistantService.share(this.assistantToShare!, selection).subscribe(response => {
			this.alertService.postSuccess(response);
			this.assistantToShare = undefined;
		});
	}
}
