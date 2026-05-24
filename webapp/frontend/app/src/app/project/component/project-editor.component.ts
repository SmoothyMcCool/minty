import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../project.service';
import { ProjectNodeComponent } from './project-node.component';
import { NodeViewerComponent } from './project-node-viewer.component';
import { AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { ProjectNode } from '../../model/project/project-node';
import { Project } from '../../model/project/project';
import { DocProperties } from '../../document/document-editor.component';
import { AssistantListComponent } from '../../assistant/component/assistant-list.component';
import { Assistant } from '../../model/assistant';
import { ConversationService } from '../../conversation.service';
import { Conversation } from '../../model/conversation/conversation';
import { ConversationListComponent } from '../../conversation/component/conversation-list.component';
import { ConversationViewerComponent } from '../../conversation/component/conversation-viewer.component';

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, NodeViewerComponent, ConfirmationDialogComponent, AssistantListComponent, ConversationListComponent, ConversationViewerComponent],
	templateUrl: 'project-editor.component.html',
	styleUrl: 'project-editor.component.css'
})
export class ProjectEditorComponent {

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

	nodes: ProjectNode[] = [];
	selectedNode: ProjectNode | undefined = undefined;

	conversations: Conversation[] = [];
	selectedConversation: Conversation | undefined = undefined;

	editFile: boolean = false;
	currentFileContents: string | undefined = undefined;

	confirmDeleteNodeVisible = false;
	nodeToDelete: ProjectNode | undefined = undefined;

	mdFileDialogVisible = false;
	zipFileDialogVisible = false
	mermaidFileDialogVisible = false;
	newChatDialogVisible = false;
	document: DocProperties = {
		title: '',
		file: undefined
	};

	constructor(private projectService: ProjectService,
		private conversationService: ConversationService,
		private alertService: AlertService) { }

	refresh() {
		this.nodes = [];
		this.selectedNode = undefined;
		this.editFile = false;
		if (this.project) {
			this.projectService.describeTree(this.project.id).subscribe((nodes: ProjectNode[]) => {
				this.nodes = nodes;
			});

			this.conversationService.listForProject(this.project.id).subscribe((conversations: Conversation[]) => {
				this.conversations = conversations;
			});
		}
	}

	startConversation(event: { assistant: Assistant, projectId: string }): void {
		this.conversationService.createInProject(event.assistant, event.projectId).subscribe( conversation => {
			this.conversationService.listForProject(event.projectId).subscribe(conversations => {
				this.conversations = conversations;
				this.onConversationSelected(conversation);
			})
		});
		this.newChatDialogVisible = false;
	}

	onConversationSelected(conversation: Conversation) {
		this.selectedNode = undefined;
		this.selectedConversation = conversation;
	}

	onConversationChanged(conversation: Conversation) {
		this.conversations = this.conversations.map(c => c.id === conversation.id ? { ...c, ...conversation } : c );
		this.selectedConversation = this.conversations.find(c => c.id === conversation.id);
	}

	onNodeSelected(node: ProjectNode) {
		if (node.type !== 'Folder') {
			this.selectedNode = node;
			this.selectedConversation = undefined;
			if (this.selectedNode) {
				this.projectService.readNode(this.project.id, this.selectedNode.path).subscribe(node => {
					this.selectedNode = node;
				});
			}
		} else {
			this.selectedNode = node;
		}
	}

	onUpdateNode(updatedNode: ProjectNode) {
		if (!this.selectedNode) {
			return;
		}

		this.projectService.updateNodeMetadata(
			this.project.id,
			this.selectedNode.path,
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
		if (!this.selectedNode) {
			console.error('editCurrentFile: selectedNode not set');
			return;
		}
		this.currentFileContents = this.selectedNode.content;
	}

	cancelEditingCurrentFile() {
		this.editFile = false;
	}

	onFileContentsChanged(node: ProjectNode) {
		this.currentFileContents = node.content;
	}

	saveChangesToCurrentFile() {
		if (!this.selectedNode) {
			return;
		}

		const currentNode = this.selectedNode;
		this.selectedNode.content = this.currentFileContents;
		this.projectService.writeFile(this.project.id, this.selectedNode).subscribe(() => {
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

	addAndConvertToMd() {
		this.mdFileDialogVisible = true;
	}

	addAndConvertToMermaid() {
		this.mermaidFileDialogVisible = true;
	}

	addMarkdownFile() {
		this.mdFileDialogVisible = false;
		try {
			this.projectService.convertAndAddMarkdown(this.project.id, this.document).subscribe((result: string) => {
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
			this.projectService.decomposeMarkdown(this.project.id, this.document).subscribe((result: string) => {
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
			this.projectService.decomposeAndSummarizeMarkdown(this.project.id, this.document).subscribe((result: string) => {
				this.alertService.postSuccess(result);
				this.refresh();
			});
		} catch (error: unknown) {
			this.alertService.postAlert({ type: 'failure', message: "Couldn't process your file. Did you forget to choose one?" });
		}
	}

	addZipToProject() {
		this.zipFileDialogVisible = true;
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

	convertToMermaid() {
		this.mermaidFileDialogVisible = false;
		try {
			this.projectService.convertToMermaid(this.project.id, this.document).subscribe((result: string) => {
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

	rootNodes(): ProjectNode[] {
		return this.nodes && this.nodes.filter(node => node.path.split('/').length === 2 && node.path != '/');
	}

	randomId(length: number) {
		return Math.random().toString(36).substring(2, length + 2);
	}

	newChat() {
		this.newChatDialogVisible = true;
	}
}
