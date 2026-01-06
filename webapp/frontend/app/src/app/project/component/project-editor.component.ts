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
	entries: NodeInfo[];

	selectedEntryInfo: NodeInfo;
	selectedEntry: Node;
	editFile: boolean = false;
	currentFileContents: string;

	confirmDeleteNodeVisible = false;
	entryInfoToDelete: NodeInfo;

	constructor(private projectService: ProjectService) {
	}

	refresh() {
		this.entries = [];
		this.selectedEntryInfo = null;
		this.selectedEntry = null;
		this.editFile = false;
		if (this.project) {
			this.projectService.listProjectEntries(this.project.id).subscribe((entries: NodeInfo[]) => {
				this.entries = entries;
			});
		}
	}

	onSelect(node: NodeInfo) {
		if (node.type !== 'Folder') {
			this.selectedEntryInfo = node;
			this.projectService.getNode(this.project.id, this.selectedEntryInfo).subscribe(entry => {
				this.selectedEntry = entry;
			});
		} else {
			this.selectedEntryInfo = null;
		}
	}

	onUpdateEntryInfo(node: NodeInfo) {
		this.projectService.getNode(this.project.id, node).subscribe(entry => {
			entry.info = node;
			this.projectService.addOrUpdateNode(this.project.id, entry).subscribe(() => {
				this.refresh();
			});
		})
	}

	onDeleteEntryInfo(node: NodeInfo) {
		this.entryInfoToDelete = node;
		this.confirmDeleteNodeVisible = true;
	}
	confirmDeleteNode() {
		this.confirmDeleteNodeVisible = false;
		this.projectService.deleteNode(this.project.id, this.entryInfoToDelete).subscribe(() => {
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
		this.currentFileContents = this.selectedEntry.data;
	}

	cancelEditingCurrentFile() {
		this.editFile = false;
	}

	onFileContentsChanged(text: string) {
		this.currentFileContents = text;
	}

	saveChangesToCurrentFile() {
		this.selectedEntry.data = this.currentFileContents;
		this.projectService.addOrUpdateNode(this.project.id, this.selectedEntry).subscribe(() => {
			this.refresh();
		});
	}

	addFile() {
		const entryInfo: NodeInfo = {
			nodeId: null,
			type: 'File',
			name: crypto.randomUUID(),
			parentId: null
		}
		const entry: Node = {
			info: entryInfo,
			data: ''
		}
		this.projectService.addOrUpdateNode(this.project.id, entry).subscribe(() => {
			this.refresh();
		});
	}

}
