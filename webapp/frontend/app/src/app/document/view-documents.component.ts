import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { DocumentService } from '../document.service';
import { ConfirmationDialogComponent } from '../app/component/confirmation-dialog.component';
import { MintyDoc } from '../model/minty-doc';
import { UserService } from '../user.service';
import { AssistantService } from '../assistant.service';
import { Alert, AlertService } from '../alert.service';
import { CommonModule } from '@angular/common';
import { DocProperties, DocumentEditorComponent } from './document-editor.component';
import { FormsModule } from '@angular/forms';
import { Assistant } from '../model/assistant';

@Component({
	selector: 'minty-view-documents',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent, DocumentEditorComponent],
	templateUrl: 'view-documents.component.html',
	styleUrls: ['view-documents.component.css']
})
export class ViewDocumentsComponent implements OnInit {

	documents: MintyDoc[] = [];
	displayDocuments: MintyDoc[] = [];

	deleteInProgress = false;
	confirmDeleteDocumentDialogVisible = false;
	documentToDelete: MintyDoc = null;

	models: string[] = [];
	assistants: Assistant[] = [];
	addingDocument = false;
	newDocument: MintyDoc = {
		title: '',
		state: '',
		documentId: '',
		model: '',
		ownerId: '',
		associatedAssistantIds: []
	};
	docProperties: DocProperties = {
		title: '',
		model: '',
		file: undefined
	};

	constructor(private documentService: DocumentService,
		private userService: UserService,
		private assistantService: AssistantService,
		private alertService: AlertService
	) {
	}

	ngOnInit(): void {
		this.documentService.list().subscribe(documents => {
			this.documents = documents;
			this.displayDocuments = documents;
		});
		this.assistantService.list().subscribe(assistants => {
			this.assistants = assistants;
		});
		this.assistantService.models().subscribe(models => {
			this.models = models;
		});
	}

	addNewDocument() {
		this.addingDocument = true;
	}

	cancelNewDocument() {
		this.addingDocument = false;
		this.newDocument = {
			title: '',
			state: '',
			documentId: '',
			model: '',
			ownerId: '',
			associatedAssistantIds: []
		};
		this.docProperties = {
			title: '',
			model: '',
			file: undefined
		};
	}

	submitNewDocument() {
		this.newDocument.title = this.docProperties.title;
		this.newDocument.model = this.docProperties.model;
		this.newDocument.state = 'NO_CONTENT';
		if (!this.docProperties.file || !this.newDocument.model || !this.newDocument.title) {
			this.alertService.postFailure("You have to give a title, model, and actual file, k?");
			return;
		}

		this.documentService.add(this.newDocument).subscribe(newDoc => {
			this.documentService.upload(newDoc.documentId, this.docProperties.file).subscribe(() => {
				// Calling this now just cleans things up for the next go-around.
				this.cancelNewDocument();
				this.documentService.list().subscribe(documents => {
					this.documents = documents;
				});
			})
		});
	}

	deleteDocument(document: MintyDoc) {
		this.confirmDeleteDocumentDialogVisible = true;
		this.documentToDelete = document;
	}

	confirmDeleteDocument() {
		this.deleteInProgress = true;
		this.confirmDeleteDocumentDialogVisible = false;

		this.documentService.delete(this.documentToDelete).subscribe(() => {
			this.deleteInProgress = false;

			this.documentService.list().subscribe(documents => {
				this.documents = documents;
			});
		});
	}

	isOwned(ownerId: number) {
		return this.userService.getUser().id == ownerId;
	}

	findAssistant(assistantId: number): Assistant {
		const assistant = this.assistants.find(assistant => assistant.id === assistantId);
		if (assistant != undefined) {
			return assistant;
		}
		return undefined;
	}

	filterChanged(filter: string) {
		this.displayDocuments = this.documents.filter(document => document.title.includes(filter));
	}
}
