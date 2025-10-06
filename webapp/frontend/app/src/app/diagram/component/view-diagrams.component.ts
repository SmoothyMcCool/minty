import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from 'src/app/user.service';
import { DisplayMode, User } from 'src/app/model/user';
import { DiagramService } from '../diagram.service';
import { Diagram } from 'src/app/model/diagram/diagram';
import { MarkdownModule } from 'ngx-markdown';


@Component({
	selector: 'minty-view-diagrams',
	imports: [CommonModule, MarkdownModule],
	templateUrl: 'view-diagrams.component.html',
	//styleUrls: ['view-diagrams.component.css']
})
export class ViewDiagramsComponent implements OnInit {

	user: User;
	DisplayMode = DisplayMode;

	conversation: string;
	response: string;
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
			this.user = user;
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
