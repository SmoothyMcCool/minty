import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ProjectNode } from "src/app/model/project/project-node";

@Component({
	selector: 'minty-project-node',
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html',
	styleUrl: 'project-node.component.css'
})
export class ProjectNodeComponent {

	@Input() node!: ProjectNode;
	@Input() selected!: ProjectNode | null;
	@Input() nodes!: ProjectNode[];
	@Output() nodeSelected = new EventEmitter<ProjectNode>();
	@Output() update = new EventEmitter<ProjectNode>();
	@Output() delete = new EventEmitter<ProjectNode>();

	editNodeInfoVisible = false;
	editName: string;
	editNodeType: 'Folder' | 'File';
	editFileType: 'code' | 'markdown' | 'json' | 'text' | 'diagram';
	editParent: string;
	isExpanded = true;

	getParentPath(input: string): string {
		if (!input) return '';
		const normalized = (input.endsWith('/') ? input.slice(0, -1) : input).replace(/\\/g, '/');
		const lastSlash = normalized.lastIndexOf('/');
		return lastSlash > 0 ? normalized.substring(0, lastSlash) : '';
	}

	getFileName(input: string): string {
		if (!input) return "";

		// Normalize Windows backslashes to forward slashes
		const normalized = input.replace(/\\/g, '/');

		// Remove trailing slash if present
		const trimmed = normalized.endsWith('/') ? normalized.slice(0, -1) : normalized;

		const lastSlash = trimmed.lastIndexOf('/');

		return lastSlash >= 0
			? trimmed.substring(lastSlash + 1)
			: trimmed;
	}

	editNodeInfo(event?: MouseEvent) {
		event?.stopPropagation();

		this.editName = this.getFileName(this.node.path);
		this.editFileType = this.node.fileType
		this.editNodeType = this.node.type;
		this.editParent = this.getParentPath(this.node.path);
		this.editNodeInfoVisible = true;
	}

	onConfirmNodeInfo() {
		const nodeInfo: ProjectNode = {
			type: this.editNodeType,
			path: (this.editParent ? this.editParent + '/' : '/') + this.editName,
			version: this.node.version + 1,
			fileType: this.editFileType,
			content: this.node.content
		};
		this.update.emit(nodeInfo);
		this.editNodeInfoVisible = false;
	}

	onCancelEditNodeInfo() {
		this.editNodeInfoVisible = false;
	}

	onSelect(node: ProjectNode) {
		// Guard against processing events from children.
		if (this.node.path === node.path && node.type === 'Folder') {
			this.toggle();
		} else {
			this.nodeSelected.emit(node);
		}
	}

	deleteNode() {
		this.delete.emit(this.node);
	}

	childNodesOf(parent: string): ProjectNode[] {
		const parentPath = parent.endsWith('/')
			? parent
			: parent + '/';

		return this.nodes.filter(node => {
			if (node.path === parent) {
				return false;
			}
			const parentParent = node.path.substring(0, node.path.lastIndexOf('/'));
			return parentParent === parent;
		});
	}

	toggle() {
		this.isExpanded = !this.isExpanded;
	}

	listFolders(): ProjectNode[] {
		return this.nodes.filter(node => node.type === 'Folder' && node.path !== '/');
	}

	stopTreePropagation(event: KeyboardEvent) {
		event.stopPropagation();
	}
}
