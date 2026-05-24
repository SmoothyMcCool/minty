import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ConversationViewerComponent } from './conversation-viewer.component';


@Component({
	selector: 'minty-view-conversation',
	imports: [CommonModule, FormsModule, ConversationViewerComponent],
	templateUrl: 'view-conversation.component.html'
})
export class ViewConversationComponent {

	conversationId: string = '';

	constructor(private route: ActivatedRoute) {
	}

	ngOnInit(): void {
		this.route.params.subscribe(params => {
			this.conversationId = params['id'];
		});
	}

}