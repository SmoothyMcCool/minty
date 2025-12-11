import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { MintyTool } from 'src/app/model/minty-tool';

@Component({
	selector: 'minty-assistant-editor',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
	templateUrl: 'assistant-editor.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => AssistantEditorComponent),
			multi: true
		}
	]
})
export class AssistantEditorComponent implements ControlValueAccessor {

	@Input() models: string[] = [];
	private _documents: MintyDoc[] = [];
	@Input()
	set documents(value: MintyDoc[]) {
		this._documents = value;
		this.assistantDocuments = this._documents.filter(doc => this.assistant.documentIds.find(id => id === doc.documentId) != undefined);
		this._documents = this._documents.filter(doc => this.assistantDocuments.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
	}
	get documents(): MintyDoc[] {
		return this._documents;
	}
	private _tools: MintyTool[] = [];
	@Input()
	set tools(value: MintyTool[]) {
		this._tools = value;
		this.assistantTools = this._tools.filter(tool => this.assistant.tools.find(name => name.localeCompare(tool.name) === 0) != undefined);
		this._tools = this._tools.filter(tool => this.assistantTools.find(asstTool => asstTool.name.localeCompare(tool.name) === 0) == undefined);
	}
	get tools(): MintyTool[] {
		return this._tools;
	}

	assistantDocuments: MintyDoc[] = [];
	assistant: Assistant;

	assistantTools: MintyTool[] = [];

	onChange: any = () => { };
	onTouched: any = () => { };

	constructor() {
	}

	modelChanged(model: string) {
		this.assistant.model = model;
		this.assistant.documentIds = [];
	}

	addDoc(doc: MintyDoc) {
		if (this.assistant.documentIds.find(el => el === doc.documentId)) {
			return;
		}
		this.assistant.documentIds.push(doc.documentId);
		this.assistantDocuments.push(doc);
		this._documents = this._documents.filter(d => d.documentId !== doc.documentId);
		// New object for better chances at sane change detection.
		this.assistant.documentIds = [...this.assistant.documentIds];
	}

	removeDoc(doc: MintyDoc) {
		this.assistant.documentIds = this.assistant.documentIds.filter(el => el !== doc.documentId);
		this.assistantDocuments = this.documents.filter(doc => this.assistant.documentIds.find(id => id === doc.documentId) != undefined);
		this.documents.push(doc);
	}

	addTool(tool: MintyTool) {
		if (this.assistant.tools.find(el => el === tool.name)) {
			return;
		}
		this.assistant.tools.push(tool.name);
		this.assistantTools.push(tool);
		this._tools = this._tools.filter(t => t.name !== tool.name);
		// New object for better chances at sane change detection.
		this.assistant.tools = [...this.assistant.tools];
	}

	removeTool(tool: MintyTool) {
		this.assistant.tools = this.assistant.tools.filter(el => el !== tool.name);
		this.assistantTools = this.tools.filter(tool => this.assistant.tools.find(id => id.localeCompare(tool.name) === 0) != undefined);
		this.tools.push(tool);
	}

	writeValue(obj: any): void {
		this.assistant = obj;
		this.tools = this.tools;
	}
	registerOnChange(fn: any): void {
		this.assistant = fn;
	}
	registerOnTouched(fn: any): void {
		this.assistant = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Nah.
	}
}
