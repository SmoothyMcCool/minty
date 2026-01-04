import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ProjectEntryInfo } from "src/app/model/project/project-entry-info";

@Component({
	selector: 'minty-project-node',
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html'
})
export class ProjectNodeComponent {

	@Input() node!: ProjectEntryInfo;
	@Input() selected!: ProjectEntryInfo | null;
	@Output() select = new EventEmitter<ProjectEntryInfo>();
	@Output() update = new EventEmitter<ProjectEntryInfo>();

	editProjectEntryInfoVisible = false;
	editName: string;
	editType: 'folder' | 'reqts' | 'design' | 'story' | 'file' | 'unknown';

	editProjectEntryInfo() {
		this.editName = this.node.name;
		this.editType = this.node.type;
		this.editProjectEntryInfoVisible = true;
	}

	onConfirmProjectEntryInfo() {
		const entryInfo: ProjectEntryInfo = {
			id: this.node.id,
			type: this.editType,
			name: this.editName,
			parent: null
		};
		this.update.emit(entryInfo);
	}

	onCancelEditProjectEntryInfo() {
		this.editProjectEntryInfoVisible = false;
	}

	onSelect() {
		this.select.emit(this.node);
	}
}