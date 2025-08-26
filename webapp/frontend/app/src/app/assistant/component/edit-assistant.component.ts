import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { DocumentService } from 'src/app/document.service';

@Component({
	selector: 'minty-edit-assistant',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
	templateUrl: 'edit-assistant.component.html'
})
export class EditAssistantComponent implements OnInit {

	models: string[] = [];
	availableDocuments: MintyDoc[] = [];
	assistantDocuments: MintyDoc[] = [];
	workingAssistant: Assistant = {
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
				this.workingAssistant = assistant;

				this.documentService.list().subscribe(docs => {
					this.availableDocuments = docs;
					this.assistantDocuments = docs.filter(doc => assistant.documentIds.find(id => id === doc.documentId) != undefined);
				});
			});
		});
		this.assistantService.models().subscribe((models: string[]) => {
			this.models = models;
		});
	}

	formInvalid(): boolean {
		return this.workingAssistant.name.length === 0 || this.workingAssistant.model.length === 0;
	}

	updateAssistant() {
		this.assistantService.update(this.workingAssistant).subscribe(() => {
			this.navigateTo('assistants');
		});
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

	addDoc(doc: MintyDoc) {
		if (this.workingAssistant.documentIds.find(el => el === doc.documentId)) {
			return;
		}
		this.workingAssistant.documentIds.push(doc.documentId);
		this.assistantDocuments.push(doc);
		// New object for better chances at sane change detection.
		this.workingAssistant.documentIds = [...this.workingAssistant.documentIds];
	}

	removeDoc(doc: MintyDoc) {
		this.workingAssistant.documentIds =
			this.workingAssistant.documentIds.filter(el => el !== doc.documentId);
		this.assistantDocuments =
			this.availableDocuments.filter(doc =>
				this.workingAssistant.documentIds.find(id =>
					id === doc.documentId) != undefined);
	}
}
