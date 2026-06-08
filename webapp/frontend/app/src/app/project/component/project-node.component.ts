import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ProjectFileType, ProjectNode, ProjectNodeType } from "../../model/project/project-node";
import { AlertService } from "../../alert.service";

@Component({
	selector: 'minty-project-node',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html',
	styleUrl: 'project-node.component.css'
})
export class ProjectNodeComponent {
	@Input() set nodes(value: ProjectNode[]) {
		this._nodes = value;
		this._updateCaches();
	}
	get nodes(): ProjectNode[] { return this._nodes; }
	private _nodes: ProjectNode[] = [];

	@Input() set node(value: ProjectNode) {
		this._node = value;
		this._fileName = this.getFileName(value.path);
		this._updateCaches();
	}
	get node(): ProjectNode { return this._node; }
	private _node!: ProjectNode;

	get children(): ProjectNode[] { return this._children; }
	get fileName(): string { return this._fileName; }

	@Input() selected!: ProjectNode | null;
	@Output() nodeSelected = new EventEmitter<ProjectNode>();
	@Output() update = new EventEmitter<ProjectNode>();
	@Output() delete = new EventEmitter<ProjectNode>();

	private _children: ProjectNode[] = [];
	private _fileName: string = '';
	private _cachedNodesRef: ProjectNode[] | null = null;

	editNodeInfoVisible = false;
	editName: string | undefined = undefined;
	editNodeType: ProjectNodeType = 'File';
	editFileType: ProjectFileType = 'text';
	editParent: string | undefined = undefined;
	isExpanded = true;

	public constructor(private alertService: AlertService) { }

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

		const final = lastSlash >= 0
			? trimmed.substring(lastSlash + 1)
			: trimmed;

		return final ? final : '/';
	}

	editNodeInfo(event?: MouseEvent) {
		event?.stopPropagation();
		this.editName = this.getFileName(this.node.path);
		this.editFileType = this.node.fileType || 'text';
		this.editNodeType = this.node.type;
		this.editParent = this.getParentPath(this.node.path);
		this.editNodeInfoVisible = true;
	}

	onConfirmNodeInfo() {
		const nodeInfo: ProjectNode = {
			type: this.editNodeType as any,
			path: (this.editParent ? this.editParent + '/' : '/') + this.editName,
			version: this.node.version + 1,
			fileType: this.editFileType as any,
			content: this.node.content
		};
		this.update.emit(nodeInfo);
		this.editNodeInfoVisible = false;
	}

	onCancelEditNodeInfo() {
		this.editNodeInfoVisible = false;
	}

	onSelect(node: ProjectNode) {
		if (this.node.path === node.path && this.node.type === 'Folder') {
			this.toggle();
		}
		this.nodeSelected.emit(node);
	}

	deleteNode() {
		if (this.node.path === '/') {
			this.alertService.postAlert({ type: 'failure', message: 'You can\'t delete the root node.' });
		} else {
			this.delete.emit(this.node);
		}
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
		return this._nodes.filter(node => node.type === 'Folder' && node.path !== '/');
	}

	stopTreePropagation(event: KeyboardEvent) {
		event.stopPropagation();
	}

	private _updateCaches(): void {
		if (!this._node || !this._nodes) return;
		this._children = this._nodes.filter(n =>
			n.path !== this._node.path &&
			n.path.startsWith(this._node.path + '/') &&
			n.path.split('/').length === this._node.path.split('/').length + 1
		);
	}

}
