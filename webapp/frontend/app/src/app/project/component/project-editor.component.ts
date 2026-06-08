import { ChangeDetectorRef, Component, Input, NgZone, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../project.service';
import { ProjectNodeComponent } from './project-node.component';
import { NodeViewerComponent } from './project-node-viewer.component';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { ProjectNode } from '../../model/project/project-node';
import { Project } from '../../model/project/project';
import { DocProperties, DocumentEditorComponent } from '../../document/document-editor.component';
import { AssistantListComponent } from '../../assistant/component/assistant-list.component';
import { Assistant } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { Conversation } from '../../model/conversation/conversation';
import { ConversationListComponent } from '../../conversation/component/conversation-list.component';
import { ConversationViewerComponent } from '../../conversation/component/conversation-viewer.component';
import { forkJoin, interval, startWith, Subscription, switchMap } from 'rxjs';
import { MintyDoc } from '../../model/minty-doc';
import { DocumentService } from '../../document.service';

const ProjectItemTypes = [
	'Conversation',
	'ProjectNode',
	'Document',
] as const;

type ProjectItemType =
	typeof ProjectItemTypes[number];

interface ItemSelection {
	type: ProjectItemType;
	item: ProjectNode | Conversation | MintyDoc | undefined;
}

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, NodeViewerComponent, ConfirmationDialogComponent, AssistantListComponent, ConversationListComponent, ConversationViewerComponent, DocumentEditorComponent],
	templateUrl: 'project-editor.component.html',
	styleUrl: 'project-editor.component.css'
})
export class ProjectEditorComponent implements OnInit, OnDestroy {

	private _project: Project | undefined = undefined;
	@Input()
	set project(value: Project) {
		if (value) {
			this._project = value;
			this.refresh();
		}
	}
	get project(): Project {
		return this._project!;
	}

	onChange = (_: any) => { };
	onTouched: any = () => { };

	readonly RootFolders: ProjectNode[] = [
		{ path: 'Conversations', version: 1, type: 'Folder' },
		{ path: 'Workflows', version: 1, type: 'Folder' },
		{ path: 'Files', version: 1, type: 'Folder' }
	];

	sidebarVisible: boolean = true;

	confirmDeleteProjectVisible = false;
	projectPendingDeletion: Project | undefined = undefined;

	selectedItem: ItemSelection | undefined = undefined;

	// -------------------------
	// FILES
	// -------------------------
	rootNodes: ProjectNode[] = [];
	nodes: ProjectNode[] = [];

	editFile: boolean = false;
	currentFileContents: string | undefined = undefined;

	confirmDeleteNodeVisible = false;
	nodeToDelete: ProjectNode | undefined = undefined;

	// -------------------------
	// CONVERSATIONS
	// -------------------------
	conversations: Conversation[] = [];

	// -------------------------
	// DOCUMENTS
	// -------------------------
	documents: MintyDoc[] = [];
	mdFileDialogVisible = false;
	zipFileDialogVisible = false
	mermaidFileDialogVisible = false;
	newConversationDialogVisible = false;
	document: DocProperties = {
		title: '',
		file: undefined
	};
	decomposeDocument = false;
	summarizeDocument = false;

	documentToDelete: MintyDoc | undefined = undefined;
	confirmDeleteDocumentVisible = false;

	processingTaskSubscription: Subscription | undefined;
	tasks: string[] = [];
	anyTaskCompleted: boolean = false;

	constructor(private ngZone: NgZone,
		private cdr: ChangeDetectorRef,
		private projectService: ProjectService,
		private documentService: DocumentService,
		private conversationService: ConversationService,
		private alertService: AlertService) { }

	ngOnInit(): void {
		this.ngZone.runOutsideAngular(() => {
			this.processingTaskSubscription = interval(5000)
				.pipe(
					startWith(0),
					switchMap(() => this.documentService.listTasks())
				)
				.subscribe(tasks => {
					const removed = this.tasks.filter(t => !this.tasks.some(t2 => t2 === t));
					const added = tasks.filter(t => !this.tasks.some(t2 => t2 === t));
					if (removed.length > 0 || added.length > 0) {
						if (removed.length > 0) {
							this.anyTaskCompleted = true;
						}
						this.tasks = tasks;
						this.ngZone.run(() => { }); // re-enter zone only when tasks actually changed
					}
					// If nothing changed, we never re-enter the zone at all
				});
		});
	}

	ngOnDestroy(): void {
		this.processingTaskSubscription?.unsubscribe();
	}

	randomId(length: number) {
		return Math.random().toString(36).substring(2, length + 2);
	}

	deleteProject(project: Project) {
		this.projectPendingDeletion = project;
		this.confirmDeleteProjectVisible = true;
	}

	confirmDeleteProject() {
		this.confirmDeleteProjectVisible = false;
		this.projectService.deleteProject(this.projectPendingDeletion!.id).subscribe();
	}

	toggleSidebar() {
		this.sidebarVisible = !this.sidebarVisible;
	}

	refresh() {
		this.nodes = [];
		this.selectedItem = undefined;
		this.editFile = false;

		forkJoin([
			this.projectService.describeTree(this.project.id),
			this.documentService.list(this.project.id),
			this.conversationService.listForProject(this.project.id)
		]).subscribe(([nodes, documents, conversations]) => {
			this.nodes = nodes;
			this.rootNodes = nodes.filter(node => node.path.split('/').length === 2 && node.path !== '/');
			this.documents = documents;
			this.conversations = conversations;

			const displayItem = this.projectService.initialDisplayItem;
			if (displayItem) {
				switch (displayItem.type) {
					case 'conversation':
						const conversation = this.conversations.find(item => item.id === displayItem.id);
						if (conversation) {
							this.onConversationSelected(conversation);
						}
						break;
					case 'file':
						const node = this.nodes.find(item => item.path === displayItem.id);
						if (node) {
							this.onNodeSelected(node);
						}
						break;
					case 'workflow':
						break;
				}
			}
		});
	}

	// -------------------------
	// CONVERSATIONS
	// -------------------------
	startConversation(event: { assistant: Assistant, projectId: string }): void {
		this.conversationService.createInProject(event.assistant, event.projectId).subscribe(conversation => {
			this.conversationService.listForProject(event.projectId).subscribe(conversations => {
				this.conversations = conversations;
				this.onConversationSelected(conversation);
			})
		});
		this.newConversationDialogVisible = false;
	}

	onConversationSelected(conversation: Conversation) {
		this.selectedItem = { type: 'Conversation', item: conversation };
	}

	onConversationChanged(conversation: Conversation) {
		this.conversations = this.conversations.map(c => c.id === conversation.id ? { ...c, ...conversation } : c);
		this.selectedItem = { type: 'Conversation', item: this.conversations.find(c => c.id === conversation.id) };
	}

	conversationsChanged(conversations: Conversation[]) {
		if (this.selectedItem?.type === 'Conversation') {
			if (!conversations.find(conversation => (this.selectedItem?.item as Conversation).id === conversation.id)) {
				this.selectedItem = undefined;
			}
		}
	}

	// -------------------------
	// FILES
	// -------------------------
	onNodeSelected(node: ProjectNode) {
		if (node.type !== 'Folder') {
			this.selectedItem = undefined;
			if (node) {
				this.projectService.readNode(this.project.id, node.path).subscribe(node => {
					this.selectedItem = { type: 'ProjectNode', item: node };
				});
			}
		} else {
			this.selectedItem = { type: 'ProjectNode', item: node };
		}
	}

	onUpdateNode(updatedNode: ProjectNode) {
		if (!this.selectedItem || this.selectedItem.type !== 'ProjectNode' || !this.selectedItem.item) {
			return;
		}

		this.projectService.updateNodeMetadata(
			this.project.id,
			(this.selectedItem.item as ProjectNode).path,
			updatedNode.path,
			updatedNode.fileType
		).subscribe(() => {
			this.refresh();
		});
	}

	onDeleteNode(node: ProjectNode) {
		this.nodeToDelete = node;
		this.confirmDeleteNodeVisible = true;
	}

	confirmDeleteNode() {
		this.confirmDeleteNodeVisible = false;
		if (!this.nodeToDelete) {
			console.error('confirmDeleteNode: nodeToDelete not set');
			return;
		}
		this.projectService.deleteNode(this.project.id, this.nodeToDelete.path).subscribe(() => {
			this.refresh();
		});
	}

	editCurrentFile() {
		this.editFile = true;
		if (!this.selectedItem || this.selectedItem.type !== 'ProjectNode' || !this.selectedItem.item) {
			console.error('editCurrentFile: selectedNode not set');
			return;
		}
		this.currentFileContents = (this.selectedItem.item as ProjectNode).content;
	}

	cancelEditingCurrentFile() {
		this.editFile = false;
	}

	onFileContentsChanged(node: ProjectNode) {
		this.currentFileContents = node.content;
	}

	saveChangesToCurrentFile() {
		if (!this.selectedItem || this.selectedItem.type !== 'ProjectNode' || !this.selectedItem.item) {
			return;
		}

		const currentNode = this.selectedItem.item as ProjectNode;
		currentNode.content = this.currentFileContents;
		this.projectService.writeFile(this.project.id, currentNode).subscribe(() => {
			this.refresh();
			this.onNodeSelected(currentNode);
		});
	}

	addFile() {
		const node: ProjectNode = {
			type: 'File',
			fileType: 'text',
			path: `/new-file-${this.randomId(6)}.txt`,
			version: 0,
			content: ''
		};

		this.projectService.writeFile(this.project.id, node).subscribe(() => {
			this.projectService.deleteNode(this.project.id, this.nodeToDelete!.path).subscribe(() => {
				this.refresh();
				this.alertService.postSuccess('Added file ' + node.path);
			});
		});
	}

	addZip() {
		this.zipFileDialogVisible = false;
		try {
			this.projectService.writeZipFile(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	downloadProject(project: Project) {
		this.projectService.downloadProjectZip(project.id);
	}

	fileSelected(event: Event) {
		const newFiles = (event.target as HTMLInputElement).files;
		if (newFiles && newFiles.length > 0) {
			this.document = { title: newFiles[0].name, file: newFiles[0] };
		}
	}

	addFolder() {
		const folderName = `/new-folder-${this.randomId(6)}`;
		this.projectService.createFolder(this.project.id, folderName).subscribe(() => {
			this.refresh();
			this.alertService.postSuccess('Added folder ' + folderName);
		});
	}

	// -------------------------
	// DOCUMENTS
	// -------------------------

	onDocumentSelected(document: MintyDoc) {
		this.selectedItem = { type: 'Document', item: document };
	}

	addMarkdownFile() {
		this.mdFileDialogVisible = false;
		try {
			this.documentService.convertAndAddMarkdown(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	decomposeMarkdown() {
		this.mdFileDialogVisible = false;
		try {
			this.documentService.decomposeMarkdown(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	decomposeAndSummarizeMarkdown() {
		this.mdFileDialogVisible = false;
		try {
			this.documentService.decomposeAndSummarizeMarkdown(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	convertToMermaid() {
		this.mermaidFileDialogVisible = false;
		try {
			this.documentService.convertToMermaid(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	confirmDeleteDocument() {
		if (this.documentToDelete) {
			this.documentService.delete(this.documentToDelete).subscribe(_ => {
				this.refresh();
			});
		}
	}

}
