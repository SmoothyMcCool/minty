import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProjectService } from '../project.service';
import { ProjectNodeComponent } from './project-node.component';
import { NodeViewerComponent } from './project-node-viewer.component';
import { Alert, AlertService } from '../../alert.service';
import { ConfirmationDialogComponent } from '../../app/component/confirmation-dialog.component';
import { DocProperties } from '../../document/document-editor.component';
import { ProjectNode } from '../../model/project/project-node';
import { Project } from '../../model/project/project';

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, NodeViewerComponent, ConfirmationDialogComponent],
	templateUrl: 'project-editor.component.html',
	styleUrl: 'project-editor.component.css'
})
export class ProjectEditorComponent {

	private _project!: Project;
	@Input()
		set project(value: Project) {
			if (value) {
				this._project = value;
			}
			this.refresh();
		}
		get project(): Project {
			return this._project;
		}

	onChange = (_: any) => { };
	onTouched: any = () => { };

	nodes: ProjectNode[] = [];

	selectedNode: ProjectNode | undefined = undefined;
	editFile: boolean = false;
	currentFileContents: string | undefined = undefined;

	confirmDeleteNodeVisible = false;
	nodeToDelete: ProjectNode | undefined = undefined;

	mdFileDialogVisible = false;
	zipFileDialogVisible = false
	mermaidFileDialogVisible = false;
	document: DocProperties = {
		title: '',
		file: undefined
	};

	constructor(private projectService: ProjectService, private alertService: AlertService) {
	}

	refresh() {
		this.nodes = [];
		this.selectedNode = undefined;
		this.editFile = false;
		if (this.project) {
			this.projectService.describeTree(this.project.id).subscribe((nodes: ProjectNode[]) => {
				this.nodes = nodes;
			});
		}
	}

	onSelected(node: ProjectNode) {
		if (node.type !== 'Folder') {
			this.selectedNode = node;
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

		this.projectService.updateNodeMetadata(this.project.id, this.selectedNode.path, updatedNode.path, updatedNode.fileType).subscribe(() => {
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

	onFileContentsChanged(text: string) {
		this.currentFileContents = text;
	}

	saveChangesToCurrentFile() {
		if (!this.selectedNode) {
			return;
		}

		const currentNode = this.selectedNode;
		this.selectedNode.content = this.currentFileContents;
		this.projectService.writeFile(this.project.id, this.selectedNode).subscribe(() => {
			this.refresh();
			this.onSelected(currentNode);
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
			this.refresh();
			this.alertService.postSuccess('Added file ' + node.path);
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
		return this.nodes && this.nodes.filter(node => node.path.split('/').length === 2);
	}

	randomId(length: number) {
		return Math.random().toString(36).substring(2, length + 2);
	};
}
