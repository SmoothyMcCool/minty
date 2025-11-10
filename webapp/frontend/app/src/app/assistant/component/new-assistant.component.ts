import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { DocumentService } from '../../document.service';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { AssistantEditorComponent } from './assistant-editor.component';

@Component({
	selector: 'minty-new-assistant',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, AssistantEditorComponent],
	templateUrl: 'new-assistant.component.html',
	styleUrls: ['new-assistant.component.css']
})
export class NewAssistantComponent implements OnInit {

	availableDocuments: MintyDoc[] = [];
	models: string[] = [];
	assistant: Assistant = {
		id: '',
		name: '',
		prompt: '',
		model: '',
		temperature: 0,
		topK: 5,
		ownerId: '',
		shared: false,
		hasMemory: false,
		documentIds: []
	};
	assistantDocuments: MintyDoc[] = [];

	constructor(
		private assistantService: AssistantService,
		private documentService: DocumentService,
		private router: Router) {
	}

	ngOnInit(): void {
		this.assistantService.models().subscribe((models: string[]) => {
			this.models = models;
		});
		this.documentService.list().subscribe(docs => {
			this.availableDocuments = docs;
		});
	}

	formInvalid(): boolean {
		return this.assistant.name.length === 0 || this.assistant.model.length === 0;
	}

	createAssistant() {
		this.assistantService.create(this.assistant).subscribe(() => {
			this.navigateTo('assistants');
		});
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

}
