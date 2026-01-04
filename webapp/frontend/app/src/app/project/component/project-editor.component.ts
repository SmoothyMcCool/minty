import { CommonModule } from '@angular/common';
import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Project } from 'src/app/model/project/project';
import { ProjectService } from '../project.service';
import { ProjectEntryInfo } from 'src/app/model/project/project-entry-info';
import { ProjectEntry } from 'src/app/model/project/project-entry';
import { ProjectNodeComponent } from './project-node.component';
import { ProjectEntryViewerComponent } from './project-entry-viewer.component';

@Component({
	selector: 'minty-project-editor',
	imports: [CommonModule, FormsModule, ProjectNodeComponent, ProjectEntryViewerComponent],
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
	entries: ProjectEntryInfo[];

	selectedEntryInfo: ProjectEntryInfo;
	selectedEntry: ProjectEntry;
	editFile: boolean = false;
	currentFileContents: string;

	constructor(private projectService: ProjectService) {
	}

	refresh() {
		this.entries = [];
		this.selectedEntryInfo = null;
		this.selectedEntry = null;
		this.editFile = false;
		if (this.project) {
			this.projectService.listProjectEntries(this.project.id).subscribe((entries: ProjectEntryInfo[]) => {
				this.entries = entries;
			});
		}
	}

	onSelect(node: ProjectEntryInfo) {
		if (node.type !== 'folder') {
			this.selectedEntryInfo = node;
			this.projectService.getProjectEntry(this.project.id, this.selectedEntryInfo).subscribe(entry => {
				this.selectedEntry = entry;
			});
		} else {
			this.selectedEntryInfo = null;
		}
	}

	onUpdateEntryInfo(node: ProjectEntryInfo) {
		this.projectService.getProjectEntry(this.project.id, node).subscribe(entry => {
			entry.info = node;
			this.projectService.addOrUpdateProjectEntry(this.project.id, entry).subscribe(() => {
				this.refresh();
			});
		})
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
		this.projectService.addOrUpdateProjectEntry(this.project.id, this.selectedEntry).subscribe(() => {
			this.refresh();
		});
	}

	addFile() {
		const entryInfo: ProjectEntryInfo = {
			id: null,
			type: 'file',
			name: crypto.randomUUID(),
			parent: null
		}
		const entry: ProjectEntry = {
			info: entryInfo,
			data: ''
		}
		this.projectService.addOrUpdateProjectEntry(this.project.id, entry).subscribe(() => {
			this.refresh();
		});
	}

}
