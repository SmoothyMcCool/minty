import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AssistantService } from '../../assistant.service';
import { ConversationService } from '../../conversation.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { Conversation } from '../../model/conversation/conversation';
import { FilterPipe } from '../../pipe/filter-pipe';
import { PredicatePipe } from '../../pipe/predicate-pipe';
import { ProjectService } from '../../project/project.service';
import { Project } from '../../model/project/project';
import { Subscription } from 'rxjs';

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
export class ConversationListComponent implements OnInit, ControlValueAccessor {

	@Input() projectMode: boolean = false;
	@Output() showConversation = new EventEmitter<Conversation>();

	conversations: Conversation[] = [];
	projects: Project[] = [];
	projectSubscription: Subscription | undefined = undefined;

	onChange = (_: any) => { };
	onTouched: any = () => { };

	conversationSortOrder: string = 'lastUsed';

	deleteInProgress = false;

	confirmDeleteConversationVisible = false;
	conversationPendingDeletionId: string  | undefined = undefined;

	conversationToEdit: Conversation | undefined = undefined;
	renamedConversationTitle: string  | undefined = undefined;
	renameConversationVisible = false;

	associateProjectVisible = false;
	projectIdToAssociate: string | undefined = undefined;

	constructor(
		private conversationService: ConversationService,
		private projectService: ProjectService,
		private assistantService: AssistantService) {
	}

	ngOnInit() {
		this.projectSubscription = this.projectService.projectList$.subscribe(projects => {
			this.projects = projects;
		});
	}

	ngOnDestroy(): void {
		this.projectSubscription?.unsubscribe();
		this.projectSubscription = undefined;
	}

	writeValue(conversations: Conversation[]): void {
		this.conversations = conversations;
		if (this.conversations) {
			this.sortConversationsBy(this.conversationSortOrder);
		}
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
			this.onTouched();
			this.onChange(this.conversations);
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

	getProjectName(conversation: Conversation): string {
		const project = this.projects.find(project => project.id === conversation.projectId);
		if (project) {
			return project.name;
		}
		return 'No associated project';
	}

	renameConversation(conversation: Conversation) {
		this.renameConversationVisible = true;
		this.conversationToEdit = conversation;
		this.renamedConversationTitle = conversation.title;
	}

	onConfirmConversationRename() {
		if (this.conversationToEdit) {
			this.conversationToEdit.title = this.renamedConversationTitle!;
			this.renameConversationVisible = false;
			this.conversationService.rename(this.conversationToEdit!).subscribe();
		}
	}

	associateProject(conversation: Conversation) {
		this.associateProjectVisible = true;
		this.conversationToEdit = conversation;
	}

	onConfirmAssociateProject() {
		if (this.conversationToEdit && this.projectIdToAssociate) {
			this.conversationToEdit.projectId = this.projectIdToAssociate;
			this.associateProjectVisible = false;
			this.conversationService.associateProject(this.conversationToEdit).subscribe();
		}
	}
}
