import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ChatMessage } from '../../model/conversation/chat-message';
import { ChatMessageComponent } from './chat-message.component';
import { FormsModule } from '@angular/forms';

@Component({
	selector: 'minty-conversation',
	imports: [CommonModule, FormsModule, ChatMessageComponent],
	templateUrl: 'conversation.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ConversationComponent {
	useMermaid = true;
	useMarkdown = true;

	@Input() messages: ChatMessage[];
	@Input() responsePending: boolean;
	@Input() queueDepth: number;

	trackByMessage(index: number, message: ChatMessage) {
		return index;
	}
};