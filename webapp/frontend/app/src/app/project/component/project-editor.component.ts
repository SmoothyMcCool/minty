import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Project } from 'src/app/model/project/project';
import { ProjectService } from '../project.service';
import { ProjectNodeComponent } from './project-node.component';
import { NodeViewerComponent } from './project-node-viewer.component';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { ProjectNode } from 'src/app/model/project/project-node';
import { DocProperties } from 'src/app/document/document-editor.component';

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, NodeViewerComponent, ConfirmationDialogComponent],
	templateUrl: 'project-editor.component.html',
})
export class ProjectEditorComponent {

	private _project: Project;
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

	nodes: ProjectNode[];

	selectedNode: ProjectNode;
	editFile: boolean = false;
	currentFileContents: string;

	confirmDeleteNodeVisible = false;
	nodeToDelete: ProjectNode;

	mdFileDialogVisible = false;
	document: DocProperties = {
		title: '',
		file: undefined
	};

	constructor(private projectService: ProjectService) {
	}

	refresh() {
		this.nodes = [];
		this.selectedNode = null;
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
			this.selectedNode = null;
		}
	}

	onUpdateNode(updatedNode: ProjectNode) {

		if (!this.selectedNode) {
			return;
		}

		this.projectService.updateNodeMetadata(this.project.id, this.selectedNode.path, updatedNode.path, updatedNode.fileType)
			.subscribe(() => {
				this.refresh();
			});
	}


	onDeleteNode(node: ProjectNode) {
		this.nodeToDelete = node;
		this.confirmDeleteNodeVisible = true;
	}

	confirmDeleteNode() {
		this.confirmDeleteNodeVisible = false;
		this.projectService.deleteNode(this.project.id, this.nodeToDelete.path).subscribe(() => {
			this.refresh();
		});
	}

	editCurrentFile() {
		this.editFile = true;
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

		this.selectedNode.content = this.currentFileContents;
		this.projectService.writeFile(this.project.id, this.selectedNode).subscribe(() => {
				this.refresh();
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
		});
	}

	addAndConvertToMd() {
		this.mdFileDialogVisible = true;
	}

	addMarkdownFile() {
		this.mdFileDialogVisible = false;
		this.projectService.convertAndAddMarkdown(this.project.id, this.document).subscribe();
	}

	markdownFileSelected(event: Event) {
		const newFiles = (event.target as HTMLInputElement).files;
		if (newFiles && newFiles.length > 0) {
			this.document = { title: newFiles[0].name, file: newFiles[0] };
		}
	}

	addFolder() {
		this.projectService.createFolder(this.project.id, `/new-folder-${this.randomId(6)}`).subscribe(() => {
			this.refresh();
		});
	}

	rootNodes(): ProjectNode[] {
		return this.nodes && this.nodes.filter(node => node.path.split('/').length === 2);
	}

	randomId(length: number) {
		return Math.random().toString(36).substring(2, length + 2);
	};
}
