import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { AssistantService } from '../../assistant.service';
import { Assistant, AssistantState } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { ConversationComponent } from './conversation.component';
import { ChatMessage } from '../../model/chat-message';

@Component({
    selector: 'ai-view-conversation',
    imports: [CommonModule, FormsModule, ConversationComponent],
    templateUrl: 'view-conversation.component.html',
    styleUrls: ['../../global.css', 'view-assistants.component.css']
})
export class ViewConversationComponent implements OnInit, OnDestroy {

    userText: string = '';
    chatHistory: ChatMessage[] = [];

    private routerSubscription: Subscription;
    private assistant: Assistant = {
        id: 0,
        name: '',
        prompt: '',
        numFiles: 0,
        state: AssistantState.READY,
        shared: false
    };
    private conversationId: string = '';

    constructor(private route: ActivatedRoute,
        private conversationService: ConversationService,
        private assistantService: AssistantService) {
        
    }

    ngOnInit(): void {
        this.route.params.subscribe(params => {
            this.conversationId = params['id'];

            this.assistantService.getAssistantForConversation(this.conversationId).subscribe((assistant: Assistant) => {
                this.assistant = assistant;
                this.conversationService.history(this.conversationId).subscribe((chatHistory: ChatMessage[]) => {
                    this.chatHistory = chatHistory;
                });
            });
        });

    }
    ngOnDestroy(): void {
        if (this.routerSubscription) {
            this.routerSubscription.unsubscribe();
        }
    }

    submit(text: string) {
        this.chatHistory.unshift({ user: true, message: text });
        this.assistantService.ask(this.conversationId, this.assistant.id, text).subscribe(response => {
            this.chatHistory.unshift({ user: false, message: response });
        });
        this.userText = '';
    }

    restart() {
        this.conversationService.delete(this.conversationId).subscribe(() => {
            this.chatHistory = [];
        })
    }
}
