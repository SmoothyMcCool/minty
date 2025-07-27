import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, AssistantState } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { ConversationService } from '../../conversation.service';
import { FilterPipe } from '../../pipe/filter-pipe';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserService } from 'src/app/user.service';

@Component({
    selector: 'minty-assistants-list',
    imports: [CommonModule, FormsModule, RouterModule, FilterPipe, ConfirmationDialogComponent],
    templateUrl: 'assistants-list.component.html'
})
export class AssistantsListComponent {

    conversations: string[] = [];
    selectedChat: string = '';

    assistants: Assistant[] = [];

    fileList: File[] = [];
    workingAssistant: Assistant = {
            id: 0,
            ownerId: 0,
            name: '',
            prompt: '',
            model: '',
            temperature: 0.9,
            numFiles: 0,
            state: AssistantState.READY,
            shared: false
        };

    deleteInProgress = false;

    confirmDeleteAssistantVisible = false;
    assistantPendingDeletion: Assistant;
    confirmDeleteConversationVisible = false;
    conversationPendingDeletionId: string;


    constructor(
        private assistantService: AssistantService,
        private conversationService: ConversationService,
        private userService: UserService,
        private router: Router) {

        this.assistantService.list().subscribe(assistants => {
            setTimeout(() => {
                this.assistants = assistants;
            }, 0);
        });

        this.conversationService.list().subscribe(conversations => {
            this.conversations = conversations;
        })
    }

    deleteAssistant(assistant: Assistant) {
        this.confirmDeleteAssistantVisible = true;
        this.assistantPendingDeletion = assistant;
    }

    confirmDeleteAssistant() {
        this.deleteInProgress = true;
        this.assistantService.delete(this.assistantPendingDeletion).subscribe(() => {
            this.assistantService.list().subscribe(assistants => {
                this.assistants = assistants;
                this.deleteInProgress = false;
            });
            this.conversationService.list().subscribe(conversations => {
                this.conversations = conversations;
            });
        });
        this.assistants = this.assistants.filter(item => item.id === this.assistantPendingDeletion.id);
    }

    startConversation(assistant: Assistant): void {
        this.conversationService.create(assistant).subscribe( conversationId => {
            this.router.navigate(['/conversation', conversationId]);
        })
    }

    selectConversation(conversationId: string) {
        this.router.navigate(['/conversation', conversationId]);
    }

    deleteConversation(conversationId: string) {
        this.confirmDeleteConversationVisible = true;
        this.conversationPendingDeletionId = conversationId;
    }

    confirmDeleteConversation() {
        this.conversationService.delete(this.conversationPendingDeletionId).subscribe(() => {
            this.assistantService.list().subscribe(assistants => {
                this.assistants = assistants;
            });
            this.conversationService.list().subscribe(conversations => {
                this.conversations = conversations;
            });
        });
        this.conversations = this.conversations.filter(item => item === this.conversationPendingDeletionId);
    }

    fileListChanged(event: Event) {
        const newFiles = (event.target as HTMLInputElement).files;
        if (newFiles !== null) {
            this.fileList = Array.from(newFiles).concat(Array.from(this.fileList));
            this.fileList = [...new Set(this.fileList)];
        }
    }

    removeFile(filename: string) {
        this.fileList = this.fileList.filter(element => element.name != filename);
    }

    isOwned(assistant: Assistant): boolean {
        return assistant.ownerId === this.userService.getUser().id;
    }
}
