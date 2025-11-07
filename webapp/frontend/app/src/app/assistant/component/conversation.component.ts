import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { ChatMessage } from '../../model/chat-message';
import { MarkdownModule } from 'ngx-markdown';
import { FormsModule } from '@angular/forms';

@Component({
	selector: 'minty-conversation',
	imports: [CommonModule, MarkdownModule, FormsModule],
	templateUrl: 'conversation.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ConversationComponent {
	useMermaid = true;
	copiedButtons = new WeakSet<HTMLElement>();

	@Input() messages: ChatMessage[];
	@Input() responsePending: boolean;
	@Input() queueDepth: number;

	onCopyClick(button: HTMLElement) {
		this.copiedButtons.add(button);
		setTimeout(() => this.copiedButtons.delete(button), 1000);
	}

	isCopied(button: HTMLElement | null): boolean {
		return !!button && this.copiedButtons.has(button);
	}
};