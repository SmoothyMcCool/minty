import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ConversationViewerComponent } from './conversation-viewer.component';
import { ConversationService } from '../../conversation.service';
import { Conversation } from '../../model/conversation/conversation';


@Component({
	selector: 'minty-view-conversation',
	imports: [CommonModule, FormsModule, ConversationViewerComponent],
	templateUrl: 'view-conversation.component.html'
})
export class ViewConversationComponent implements OnInit {

	conversation: Conversation | undefined = undefined;

	constructor(private route: ActivatedRoute,
		private conversationService: ConversationService) {
	}

	ngOnInit(): void {
		this.route.params.subscribe(params => {
			const conversationId = params['id'];
			this.conversationService.getConversation(conversationId).subscribe(conversation => {
				this.conversation = conversation;
			})
		});
	}

}