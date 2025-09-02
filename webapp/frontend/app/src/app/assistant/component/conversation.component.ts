import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ChatMessage } from '../../model/chat-message';
import { MarkdownModule } from 'ngx-markdown';

@Component({
	selector: 'minty-conversation',
	imports: [CommonModule, MarkdownModule],
	templateUrl: 'conversation.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ConversationComponent {
	@Input() messages: ChatMessage[];
};