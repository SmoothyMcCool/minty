import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Assistant, createAssistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { UserSelectDialogComponent, UserSelection } from '../../app/component/user-select-dialog.component';
import { User } from '../../model/user';
import { FilterPipe } from '../../pipe/filter-pipe';
import { PredicatePipe } from '../../pipe/predicate-pipe';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-assistant-list',
	imports: [FormsModule, RouterModule, FilterPipe, ConfirmationDialogComponent, PredicatePipe, UserSelectDialogComponent],
	templateUrl: 'assistant-list.component.html'
})
export class AssistantListComponent implements OnInit {

	@Input() projectId: string | undefined = undefined;
	@Output() conversationRequest = new EventEmitter<{ assistant : Assistant, projectId: string | undefined }>();

	assistants: Assistant[] = [];
	sharedAssistants: Assistant[] = [];
	assistantFilter: string = '';

	workingAssistant: Assistant = createAssistant();

	userSelectDialogVisible = false;
	assistantToShare: string | undefined = undefined;
	sharingSelection: UserSelection | undefined = undefined;

	onlyOwnedAssistants = false;

	user!: User;

	constructor(
		private assistantService: AssistantService,
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
		});
	}

	startConversation(assistant: Assistant): void {
		this.conversationRequest.emit({ assistant: assistant, projectId: this.projectId });
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

	isOwned(assistant: Assistant): boolean {
		return assistant.owned;
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
