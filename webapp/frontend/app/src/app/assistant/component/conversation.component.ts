import { CommonModule } from "@angular/common";
import { Component, Input } from "@angular/core";
import { ChatMessage } from "../../model/chat-message";

@Component({
    selector: 'ai-conversation',
    imports: [CommonModule],
    templateUrl: 'conversation.component.html',
    styleUrls: ['conversation.component.css']
})
export class ConversationComponent {
    @Input() messages: ChatMessage[];
};