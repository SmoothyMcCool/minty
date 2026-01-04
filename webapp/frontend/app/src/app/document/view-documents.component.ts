import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { DocumentService } from '../document.service';
import { ConfirmationDialogComponent } from '../app/component/confirmation-dialog.component';
import { MintyDoc } from '../model/minty-doc';
import { UserService } from '../user.service';
import { AssistantService } from '../assistant.service';
import { AlertService } from '../alert.service';
import { CommonModule } from '@angular/common';
import { DocProperties, DocumentEditorComponent } from './document-editor.component';
import { FormsModule } from '@angular/forms';
import { Assistant } from '../model/assistant';
import { Subscription } from 'rxjs';
import { User } from '../model/user';

@Component({
	selector: 'minty-view-documents',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent, DocumentEditorComponent],
	templateUrl: 'view-documents.component.html'
})
export class ViewDocumentsComponent implements OnInit, OnDestroy {

	user: User;

	documents: MintyDoc[] = [];
	displayDocuments: MintyDoc[] = [];

	deleteInProgress = false;
	confirmDeleteDocumentDialogVisible = false;
	documentToDelete: MintyDoc = null;

	assistants: Assistant[] = [];
	addingDocument = false;
	newDocument: MintyDoc = {
		title: '',
		state: '',
		documentId: '',
		ownerId: '',
		associatedAssistantIds: []
	};
	docProperties: DocProperties = {
		title: '',
		file: undefined
	};

	private subscription: Subscription;
	private filter: string;

	constructor(private documentService: DocumentService,
		private userService: UserService,
		private assistantService: AssistantService,
		private alertService: AlertService) {
	}

	ngOnInit(): void {
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
		this.documentService.list().subscribe(documents => {
			this.documents = documents;
			this.filterChanged(this.filter);
		});
		this.assistantService.list().subscribe(assistants => {
			this.assistants = assistants;
		});
		this.subscription = this.documentService.mintyDocListList$.subscribe((value: MintyDoc[]) => {
			this.documents = value;
			this.filterChanged(this.filter);
		});
	}

	ngOnDestroy(): void {
		if (this.subscription) {
			this.subscription.unsubscribe();
			this.subscription = undefined;
		}
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
			ownerId: '',
			associatedAssistantIds: []
		};
		this.docProperties = {
			title: '',
			file: undefined
		};
	}

	submitNewDocument() {
		this.newDocument.title = this.docProperties.title;
		this.newDocument.state = 'NO_CONTENT';
		if (!this.docProperties.file || !this.newDocument.title) {
			this.alertService.postFailure('You have to give a title and actual file, k?');
			return;
		}

		this.documentService.add(this.newDocument).subscribe(newDoc => {
			this.documentService.upload(newDoc.documentId, this.docProperties.file).subscribe(() => {
				// Calling this now just cleans things up for the next go-around.
				this.cancelNewDocument();
				this.documentService.list().subscribe(documents => {
					this.documents = documents;
					this.filterChanged(this.filter);
				});
			});
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
				this.filterChanged(this.filter); // Rerun the filter to trigger a screen refresh.
			});
		});
	}

	isOwned(ownerId: string) {
		return this.user?.id == ownerId;
	}

	findAssistant(assistantId: string): Assistant {
		const assistant = this.assistants.find(assistant => assistant.id === assistantId);
		if (assistant != undefined) {
			return assistant;
		}
		return undefined;
	}

	filterChanged(filter: string) {
		this.filter = filter;
		if (this.filter) {
			this.displayDocuments = this.documents.filter(document => document.title.includes(filter));
		} else {
			this.displayDocuments = this.documents;
		}
	}
}
