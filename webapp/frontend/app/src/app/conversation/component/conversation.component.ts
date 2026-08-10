
import { Component, Input } from '@angular/core';
import { ChatMessage } from '../../model/conversation/chat-message';
import { FormsModule } from '@angular/forms';
import { ChatMessageComponent } from './chat-message.component';
import { ThoughtEntry } from '../../model/conversation/streaming-response';

@Component({
	selector: 'minty-conversation',
	imports: [FormsModule, ChatMessageComponent],
	templateUrl: 'conversation.component.html',
	styleUrls: ['conversation.component.css'],
})
export class ConversationComponent {
	useMermaid = true;
	useMarkdown = true;

	private _thoughts!: ThoughtEntry[];
	@Input() set thoughts(value: ThoughtEntry[]) {
		this._thoughts = value;
		this.collapsedEntries.clear();
	}
	get thoughts(): ThoughtEntry[] {
		return this._thoughts;
	}
	@Input() messages!: ChatMessage[];
	@Input() responsePending!: boolean;
	@Input() responseComplete!: boolean;
	@Input() queueDepth!: number;
	@Input() oldestMessagesFirst!: boolean;
	@Input() showChatOptions!: boolean;

	isThinkingExpanded = false;
	private collapsedEntries = new Set<number>();

	trackByMessage(index: number, message: ChatMessage) {
		return index;
	}

	toggleEntry(index: number) {
		if (this.collapsedEntries.has(index)) {
			this.collapsedEntries.delete(index);
		} else {
			this.collapsedEntries.add(index);
		}
	}

	isEntryExpanded(index: number): boolean {
		return !this.collapsedEntries.has(index);
	}

};