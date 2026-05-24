import { Component, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { AssistantService } from '../../assistant.service';
import { ConversationService } from '../../conversation.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { Conversation } from '../../model/conversation/conversation';
import { FilterPipe } from '../../pipe/filter-pipe';
import { PredicatePipe } from '../../pipe/predicate-pipe';

@Component({
	selector: 'minty-conversation-list',
	imports: [FormsModule, FilterPipe, ConfirmationDialogComponent, PredicatePipe],
	templateUrl: 'conversation-list.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => ConversationListComponent),
			multi: true
		}
	]
})
export class ConversationListComponent implements ControlValueAccessor {

	@Input() projectMode: boolean = false;
	@Output() showConversation = new EventEmitter<Conversation>();

	conversations: Conversation[] = [];

	onChange = (_: any) => { };
	onTouched: any = () => { };

	selectedChat: string = '';

	conversationSortOrder: string = 'lastUsed';

	deleteInProgress = false;

	confirmDeleteConversationVisible = false;
	conversationPendingDeletionId: string  | undefined = undefined;

	conversationToRename: Conversation | undefined = undefined;
	renamedConversationTitle: string  | undefined = undefined;
	renameConversationVisible = false;

	constructor(
		private conversationService: ConversationService,
		private assistantService: AssistantService) {
	}

	writeValue(conversations: Conversation[]): void {
		this.conversations = conversations;
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

	sortConversationsBy(ordering: string) {
		this.conversationSortOrder = ordering;
		this.conversations = [...this.sortConversations(this.conversations)];
	}

	sortConversations(conversations: Conversation[]): Conversation[] {
		return conversations.sort((left, right) => {
			if (this.conversationSortOrder === 'alpha') {
				if (!left.title && !right.title) {
					return left.id.localeCompare(right.id);
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
					return left.id.localeCompare(right.id);
				}
				if (!left.lastUsed) {
					return 1;
				}
				if (!right.lastUsed) {
					return -1;
				}
				const diff = new Date(right.lastUsed).getTime() - new Date(left.lastUsed).getTime();
				return diff || left.id.localeCompare(right.id)
			}
		});
	}

	selectConversation(conversation: Conversation) {
		this.showConversation.emit(conversation);
	}

	deleteConversation(conversation: Conversation) {
		this.confirmDeleteConversationVisible = true;
		this.conversationPendingDeletionId = conversation.id;
	}

	confirmDeleteConversation() {
		this.confirmDeleteConversationVisible = false;
		this.conversationService.delete(this.conversationPendingDeletionId!).subscribe(_ => {
			this.conversations = this.conversations.filter(item => item.id != this.conversationPendingDeletionId);
		});
	}

	getConversationTitle(conversation: Conversation): string {
		if (conversation.title !== null && conversation.title.length > 0) {
			return conversation.title;
		}
		return conversation.id;
	}

	getConversationAssistantName(conversation: Conversation): string {
		const assistant = this.assistantService.find(conversation.associatedAssistantId);
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
			this.conversationService.rename(this.conversationToRename!).subscribe();
		} else {
			console.log('onConfirmConversationRename: no conversation to rename set');
		}
	}

	onCancelConversationRename() {
		this.renameConversationVisible = false;
	}

}
