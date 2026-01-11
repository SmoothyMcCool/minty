import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { MarkdownModule } from 'ngx-markdown';
import { FormsModule } from '@angular/forms';
import { ChatMessage } from 'src/app/model/conversation/chat-message';
import { MermaidClipboardDirective } from './mermaid-clipboard.directive';

@Component({
	selector: 'minty-chat-message',
	imports: [CommonModule, MarkdownModule, FormsModule, MermaidClipboardDirective],
	templateUrl: 'chat-message.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ChatMessageComponent {
	private _message: ChatMessage;
	@Input()
	get message(): ChatMessage {
		return this._message
	}
	set message(message: ChatMessage) {
		message.message = this.preProcessMessage(message.message);
		this._message = message;
	}

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

	preProcessMessage(message: string): string {
	return message.replace(
		/```mermaid([\s\S]*?)```/g,
		(_, code) => {
			const escaped = code.trim().replace(/"/g, '&quot;');
			return `
<div class="mermaid" data-mermaid-source="${escaped}">
${code.trim()}
</div>`;
		}
	);
}

};