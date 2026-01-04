import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MarkdownModule } from 'ngx-markdown';
import { FormsModule } from '@angular/forms';
import { ChatMessage } from 'src/app/model/conversation/chat-message';

@Component({
	selector: 'minty-chat-message',
	imports: [CommonModule, MarkdownModule, FormsModule],
	templateUrl: 'chat-message.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ChatMessageComponent {
	@Input() message: ChatMessage;
	@Input() useMarkdown: boolean;
	@Input() useMermaid: boolean;
	@Input() isFirst: boolean;
	@Input() responsePending: boolean;
	@Input() queueDepth?: number;

	copiedButtons = new WeakSet<HTMLElement>();

	onCopyClick(button: HTMLElement) {
		this.copiedButtons.add(button);
		setTimeout(() => this.copiedButtons.delete(button), 1000);
	}

	isCopied(button: HTMLElement | null): boolean {
		return !!button && this.copiedButtons.has(button);
	}
};