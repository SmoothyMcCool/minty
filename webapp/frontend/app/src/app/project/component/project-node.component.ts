import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { NodeInfo } from "src/app/model/project/node-info";

@Component({
	selector: 'minty-project-node',
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html',
	styleUrl: 'project-node.component.css'
})
export class ProjectNodeComponent {

	@Input() node!: NodeInfo;
	@Input() selected!: NodeInfo | null;
	@Input() nodes!: NodeInfo[];
	@Output() select = new EventEmitter<NodeInfo>();
	@Output() update = new EventEmitter<NodeInfo>();
	@Output() delete = new EventEmitter<NodeInfo>();

	editNodeInfoVisible = false;
	editName: string;
	editType: 'Folder' | 'File';
	editParent: string;
	isExpanded = true;

	editNodeInfo() {
		this.editName = this.node.name;
		this.editType = this.node.type;
		this.editParent = this.node.parentId;
		this.editNodeInfoVisible = true;
	}

	onConfirmNodeInfo() {
		const entryInfo: NodeInfo = {
			nodeId: this.node.nodeId,
			type: this.editType,
			name: this.editName,
			parentId: this.editParent
		};
		this.update.emit(entryInfo);
	}

	onCancelEditNodeInfo() {
		this.editNodeInfoVisible = false;
	}

	onSelect(nodeInfo: NodeInfo) {
		// Guard against processing events from children.
		if (this.node.nodeId === nodeInfo.nodeId && nodeInfo.type === 'Folder') {
			this.toggle();
		} else {
			this.select.emit(nodeInfo);
		}
	}

	onUpdate(nodeInfo: NodeInfo) {
		this.update.emit(nodeInfo);
	}

	onDelete(nodeInfo: NodeInfo) {
		this.delete.emit(nodeInfo);
	}

	deleteNode() {
		this.delete.emit(this.node);
	}

	childNodesOf(nodeId: String): NodeInfo[] {
		return this.nodes.filter(node => node.parentId === nodeId);
	}

	toggle() {
		this.isExpanded = !this.isExpanded;
	}

	listFolders(): NodeInfo[] {
		return this.nodes.filter(node => node.type === 'Folder');
	}
}