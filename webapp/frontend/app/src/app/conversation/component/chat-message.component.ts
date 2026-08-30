
import { Component, Input } from '@angular/core';
import { MarkdownModule } from 'ngx-markdown';
import { FormsModule } from '@angular/forms';
import { MermaidClipboardDirective } from './mermaid-clipboard.directive';
import { ChatMessage } from '../../model/conversation/chat-message';
import { SpinnerComponent } from '../../app/component/spinner.component';

@Component({
	selector: 'minty-chat-message',
	imports: [MarkdownModule, FormsModule, SpinnerComponent, MermaidClipboardDirective],
	templateUrl: 'chat-message.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ChatMessageComponent {
	private _message!: ChatMessage;
	@Input()
	get message(): ChatMessage {
		return this._message
	}
	set message(message: ChatMessage) {
		this._message = message;
	}

	@Input() useMarkdown!: boolean;
	@Input() useMermaid!: boolean;
	@Input() isFirst!: boolean;
	@Input() responsePending!: boolean;
	@Input() responseComplete!: boolean;
	@Input() queueDepth!: number;

	copiedButtons = new WeakSet<HTMLElement>();

	private expanded = false;

	toggleEntry(): void {
		this.expanded = !this.expanded;
	}

	isEntryExpanded(): boolean {
		return this.expanded;
	}

	getToolCallHeader(content: string): string {
		return content.split(/\r?\n\s*\r?\n/, 1)[0].trim();
	}

	getToolCallResult(content: string): string {
		const parts = content.split(/\r?\n\s*\r?\n/, 2);
		return parts.length > 1 ? parts[1].trim() : '';
	}

	onCopyClick(button: HTMLElement) {
		this.copiedButtons.add(button);
		setTimeout(() => this.copiedButtons.delete(button), 1000);
	}

	isCopied(button: HTMLElement | null): boolean {
		return !!button && this.copiedButtons.has(button);
	}

};