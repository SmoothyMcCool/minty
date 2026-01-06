import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { NodeInfo } from "src/app/model/project/node-info";

@Component({
	selector: 'minty-project-node',
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html'
})
export class ProjectNodeComponent {

	@Input() node!: NodeInfo;
	@Input() selected!: NodeInfo | null;
	@Output() select = new EventEmitter<NodeInfo>();
	@Output() update = new EventEmitter<NodeInfo>();
	@Output() delete = new EventEmitter<NodeInfo>();

	editNodeInfoVisible = false;
	editName: string;
	editType: 'Folder' | 'File';

	editNodeInfo() {
		this.editName = this.node.name;
		this.editType = this.node.type;
		this.editNodeInfoVisible = true;
	}

	onConfirmNodeInfo() {
		const entryInfo: NodeInfo = {
			nodeId: this.node.nodeId,
			type: this.editType,
			name: this.editName,
			parentId: null
		};
		this.update.emit(entryInfo);
	}

	onCancelEditNodeInfo() {
		this.editNodeInfoVisible = false;
	}

	onSelect() {
		this.select.emit(this.node);
	}

	deleteNode() {
		this.delete.emit(this.node);
	}
}