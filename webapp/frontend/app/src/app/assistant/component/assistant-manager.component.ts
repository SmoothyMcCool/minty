import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Assistant, createAssistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { ConversationService } from '../../conversation.service';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { UserSelectDialogComponent, UserSelection } from '../../app/component/user-select-dialog.component';
import { Conversation } from '../../model/conversation/conversation';
import { FilterPipe } from '../../pipe/filter-pipe';
import { PredicatePipe } from '../../pipe/predicate-pipe';
import { ConversationListComponent } from '../../conversation/component/conversation-list.component';
import { AssistantListComponent } from './assistant-list.component';
import { ProjectService } from '../../project/project.service';
import { Subscription } from 'rxjs';
import { Project } from '@playwright/test';

@Component({
	selector: 'minty-chat',
	imports: [FormsModule, RouterModule, FilterPipe, ConfirmationDialogComponent, PredicatePipe, UserSelectDialogComponent, AssistantListComponent, ConversationListComponent],
	templateUrl: 'assistant-manager.component.html'
})
export class AssistantManagerComponent implements OnInit, OnDestroy {

	conversations: Conversation[] = [];

	assistants: Assistant[] = [];
	sharedAssistants: Assistant[] = [];
	assistantFilter: string = '';

	workingAssistant: Assistant = createAssistant();

	conversationToRename: Conversation | undefined = undefined;
	renamedConversationTitle: string  | undefined = undefined;
	renameConversationVisible = false;

	userSelectDialogVisible = false;
	assistantToShare: string | undefined = undefined;
	sharingSelection: UserSelection | undefined = undefined;

	onlyOwnedAssistants = false;

	activeProjectSubscription: Subscription | undefined = undefined;
	activeProject: Project | undefined = undefined;

	constructor(
		private assistantService: AssistantService,
		private conversationService: ConversationService,
		private projectService: ProjectService,
		private alertService: AlertService,
		private router: Router) {
	}

	ngOnInit() {
		this.assistantService.list().subscribe(assistants => {
			setTimeout(() => {
				this.sortAssistants(assistants);
				this.assistants = assistants;
				this.sharedAssistants = assistants.filter(assistant => assistant.owned === false);
			}, 0);
		});

		this.conversationService.list().subscribe(conversations => {
			this.conversations = conversations;
		});

		this.activeProjectSubscription = this.projectService.activeProject$.subscribe(activeProject => {
			this.activeProject = activeProject;
		});
	}

	ngOnDestroy(): void {
		this.activeProjectSubscription?.unsubscribe();
		this.activeProjectSubscription = undefined;
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
		this.router.navigate(['/assistants/edit', assistantId], { queryParamsHandling: 'merge' });
	}

	startConversation(event: { assistant: Assistant, projectId: string }): void {
		if (!event.projectId) {
			this.conversationService.create(event.assistant).subscribe(conversation => {
				this.router.navigate(['/conversation', conversation.id], { queryParamsHandling: 'merge' });
			});
			return;
		}
		this.conversationService.createInProject(event.assistant, event.projectId).subscribe(conversation => {
			this.projectService.initialDisplayItem = { type: 'conversation', id: conversation.id };
			this.router.navigate(['/projects'], { queryParamsHandling: 'merge' });
		});
	}

	selectConversation(conversation: Conversation) {
		if (!conversation.projectId) {
			this.router.navigate(['/conversation', conversation.id], { queryParamsHandling: 'merge' });
			return;
		}
		this.projectService.initialDisplayItem = {type: 'conversation', id: conversation.id};
		this.router.navigate(['/projects'], { queryParams: { projectId: conversation.projectId }, queryParamsHandling: 'merge' });
	}

	isOwned(assistant: Assistant): boolean {
		return assistant.owned;
	}

	getConversationTitle(conversation: Conversation): string {
		if (conversation.title !== null && conversation.title.length > 0) {
			return conversation.title;
		}
		return conversation.id;
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

	reloadConversationList() {
		this.conversationService.list().subscribe(conversations => {
			this.conversations = conversations;
		});
	}
}
