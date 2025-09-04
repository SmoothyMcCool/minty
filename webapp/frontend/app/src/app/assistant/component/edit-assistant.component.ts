import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { DocumentService } from 'src/app/document.service';
import { AssistantEditorComponent } from './assistant-editor.component';

@Component({
	selector: 'minty-edit-assistant',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, AssistantEditorComponent],
	templateUrl: 'edit-assistant.component.html'
})
export class EditAssistantComponent implements OnInit {

	models: string[] = [];
	availableDocuments: MintyDoc[] = [];
	assistant: Assistant = {
		id: '',
		name: '',
		prompt: '',
		model: '',
		temperature: 0,
		numFiles: 0,
		ownerId: '',
		shared: false,
		hasMemory: false,
		documentIds: []
	};

	constructor(
		private route: ActivatedRoute,
		private assistantService: AssistantService,
		private documentService: DocumentService,
		private router: Router) {
	}

	ngOnInit(): void {
		this.route.params.subscribe(params => {
			this.assistantService.getAssistant(params['id']).subscribe((assistant: Assistant) => {
				this.assistant = assistant;
				this.documentService.list().subscribe(docs => {
					this.availableDocuments = docs;
				});
			});
		});
		this.assistantService.models().subscribe((models: string[]) => {
			this.models = models;
		});
	}

	formInvalid(): boolean {
		return this.assistant.name.length === 0 || this.assistant.model.length === 0;
	}

	updateAssistant() {
		this.assistantService.update(this.assistant).subscribe(() => {
			this.navigateTo('assistants');
		});
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

}
