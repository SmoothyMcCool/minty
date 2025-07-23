import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, AssistantState } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { ConversationService } from '../../conversation.service';
import { FilterPipe } from '../../pipe/filter-pipe';

@Component({
    selector: 'ai-assistants-list',
    imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
    templateUrl: 'assistants-list.component.html'
})
export class AssistantsListComponent {

    conversations: string[] = [];
    selectedChat: string = '';

    assistants: Assistant[] = [];

    fileList: File[] = [];
    workingAssistant: Assistant = {
            id: 0,
            name: '',
            prompt: '',
            model: '',
            numFiles: 0,
            state: AssistantState.READY,
            shared: false
        };

    deleteInProgress = false;

    constructor(
        private assistantService: AssistantService,
        private conversationService: ConversationService,
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
        this.deleteInProgress = true;
        this.assistantService.delete(assistant).subscribe(() => {
            this.assistantService.list().subscribe(assistants => {
                this.assistants = assistants;
                this.deleteInProgress = false;
            });
            this.conversationService.list().subscribe(conversations => {
                this.conversations = conversations;
            });
        });
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
        this.conversationService.delete(conversationId).subscribe(() => {
            this.assistantService.list().subscribe(assistants => {
                this.assistants = assistants;
            });
            this.conversationService.list().subscribe(conversations => {
                this.conversations = conversations;
            });
        });
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
}
