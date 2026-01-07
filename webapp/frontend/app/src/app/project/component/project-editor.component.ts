import { CommonModule } from '@angular/common';
import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Project } from 'src/app/model/project/project';
import { ProjectService } from '../project.service';
import { NodeInfo } from 'src/app/model/project/node-info';
import { Node } from 'src/app/model/project/node';
import { ProjectNodeComponent } from './project-node.component';
import { NodeViewerComponent } from './project-entry-viewer.component';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, NodeViewerComponent, ConfirmationDialogComponent],
	templateUrl: 'project-editor.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => ProjectEditorComponent),
			multi: true
		}
	]
})
export class ProjectEditorComponent implements ControlValueAccessor {

	onChange: any = () => { };
	onTouched: any = () => { };

	project: Project;
	nodes: NodeInfo[];

	selectedNodeInfo: NodeInfo;
	selectedNode: Node;
	editFile: boolean = false;
	currentFileContents: string;

	confirmDeleteNodeVisible = false;
	nodeInfoToDelete: NodeInfo;

	constructor(private projectService: ProjectService) {
	}

	refresh() {
		this.nodes = [];
		this.selectedNodeInfo = null;
		this.selectedNode = null;
		this.editFile = false;
		if (this.project) {
			this.projectService.listProjectEntries(this.project.id).subscribe((nodes: NodeInfo[]) => {
				this.nodes = nodes;
			});
		}
	}

	onSelected(node: NodeInfo) {
		if (node.type !== 'Folder') {
			this.selectedNodeInfo = node;
			if (this.selectedNodeInfo.nodeId) {
				this.projectService.getNode(this.project.id, this.selectedNodeInfo).subscribe(node => {
					this.selectedNode = node;
				});
			}
		} else {
			this.selectedNodeInfo = null;
		}
	}

	onUpdateNodeInfo(nodeInfo: NodeInfo) {
		this.projectService.getNode(this.project.id, nodeInfo).subscribe(node => {
			node.info = nodeInfo;
			this.projectService.addOrUpdateNode(this.project.id, node).subscribe(() => {
				this.refresh();
			});
		})
	}

	onDeleteNodeInfo(node: NodeInfo) {
		this.nodeInfoToDelete = node;
		this.confirmDeleteNodeVisible = true;
	}

	confirmDeleteNode() {
		this.confirmDeleteNodeVisible = false;
		this.projectService.deleteNode(this.project.id, this.nodeInfoToDelete).subscribe(() => {
			this.refresh();
		});
	}

	writeValue(obj: any): void {
		this.project = obj;
		this.refresh();
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Nah.
	}

	editCurrentFile() {
		this.editFile = true;
		this.currentFileContents = this.selectedNode.data;
	}

	cancelEditingCurrentFile() {
		this.editFile = false;
	}

	onFileContentsChanged(text: string) {
		this.currentFileContents = text;
	}

	saveChangesToCurrentFile() {
		this.selectedNode.data = this.currentFileContents;
		this.projectService.addOrUpdateNode(this.project.id, this.selectedNode).subscribe(() => {
			this.refresh();
		});
	}

	addFile() {
		const entryInfo: NodeInfo = {
			nodeId: null,
			type: 'File',
			name: crypto.randomUUID(),
			parentId: this.nodes.find(node => node.name === '/').nodeId
		}
		const entry: Node = {
			info: entryInfo,
			data: ''
		}
		this.projectService.addOrUpdateNode(this.project.id, entry).subscribe(() => {
			this.refresh();
		});
	}

	childNodesOf(nodeId: String): NodeInfo[] {
		return this.nodes.filter(node => node.parentId === nodeId);
	}

	rootNodes(): NodeInfo[] {
		const root = this.nodes.find(node => node.name === '/');
		return this.nodes.filter(node => node.parentId === root.nodeId);
	}
}
