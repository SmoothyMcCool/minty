import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { DocumentService } from '../../document.service';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';

@Component({
	selector: 'minty-new-assistant',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
	templateUrl: 'new-assistant.component.html',
	styleUrls: ['new-assistant.component.css']
})
export class NewAssistantComponent implements OnInit {

	availableDocuments: MintyDoc[] = [];
	models: string[] = [];
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
		return this.workingAssistant.name?.length === 0 || this.workingAssistant.model?.length === 0;
	}

	modelChanged(model: string) {
		this.workingAssistant.model = model;
		this.workingAssistant.documentIds = [];
	}

	createAssistant() {
		this.assistantService.create(this.workingAssistant).subscribe(() => {
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
