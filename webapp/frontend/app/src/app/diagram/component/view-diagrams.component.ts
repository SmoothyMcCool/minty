import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DiagramService } from '../diagram.service';
import { MarkdownModule } from 'ngx-markdown';
import { Diagram } from '../../model/diagram/diagram';
import { UserService } from '../../user.service';


@Component({
	selector: 'minty-view-diagrams',
	imports: [CommonModule, MarkdownModule],
	templateUrl: 'view-diagrams.component.html'
})
export class ViewDiagramsComponent implements OnInit {

	conversation: string | undefined = undefined;
	response: string | undefined = undefined;
	diagram: Diagram = {
		id: '',
		title: '',
		mermaid: ''
	};

	constructor(private userService: UserService,
		private diagramService: DiagramService
	) {
	}

	ngOnInit(): void {
		this.userService.getUser().subscribe(user => {
			this.conversation = 'Make me a sequence diagram of Alice saying hello to Bob.';
			this.diagramService.ask(this.conversation).subscribe(requestId => {
				this.diagramService.get(requestId).subscribe(response => {
					this.response = response;
					this.diagram = {
						id: 'abc',
						title: 'Beautiful Diagram',
						mermaid: response
					};
				})
			})
		});
	}

}
