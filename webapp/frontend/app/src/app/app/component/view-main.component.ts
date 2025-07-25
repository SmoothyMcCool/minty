import { AfterViewInit, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AssistantService } from '../../assistant.service';
import { ConversationComponent } from '../../assistant/component/conversation.component';
import { ConversationService } from '../../conversation.service';
import { ChatMessage } from '../../model/chat-message';

@Component({
    selector: 'minty-view-main',
    imports: [CommonModule, FormsModule, ConversationComponent],
    templateUrl: 'view-main.component.html',
    styleUrls: ['../../global.css', 'view-main.component.css']
})
export class ViewMainComponent implements AfterViewInit {

    queryText: string = '';
    chatHistory: ChatMessage[] = [];

    constructor(
        private assistantService: AssistantService,
        private conversationService: ConversationService) {

    }

    ngAfterViewInit(): void {
    }

    submit() {
        this.chatHistory.unshift({ user: true, message: this.queryText });
        let response = '';
        this.chatHistory.unshift({ user: false, message: response });
        this.assistantService.askDefaultAssistant(this.queryText).subscribe(responseChunk => {
            response += responseChunk
            this.chatHistory[0] = { user: false, message: response };
        });
        this.queryText = '';
    }

    restart() {
        this.conversationService.deleteDefault().subscribe(() => {
            this.chatHistory = [];
        })
    }
}
