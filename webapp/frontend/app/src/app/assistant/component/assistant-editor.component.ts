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
	private _availableDocuments: MintyDoc[] = [];
	@Input()
	set availableDocuments(value: MintyDoc[]) {
		this._availableDocuments = value;
		this.assistantDocuments = this._availableDocuments.filter(doc => this.assistant.documentIds.find(id => id === doc.documentId) != undefined);
		this._availableDocuments = this._availableDocuments.filter(doc => this.assistantDocuments.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
	}
	get availableDocuments(): MintyDoc[] {
		return this._availableDocuments;
	}
	private _tools: MintyTool[] = [];
	@Input()
	set tools(value: MintyTool[]) {
		this._tools = value;
		this.assistantTools = this._tools.filter(tool => this.assistant.tools.find(id => id === tool.name) != undefined);
		this._tools = this._tools.filter(tool => this.assistantTools.find(asstTool => asstTool.name === tool.name) == undefined);
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
		this._availableDocuments = this._availableDocuments.filter(doc => this.assistantDocuments.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
		// New object for better chances at sane change detection.
		this.assistant.documentIds = [...this.assistant.documentIds];
	}

	removeDoc(doc: MintyDoc) {
		this.assistant.documentIds = this.assistant.documentIds.filter(el => el !== doc.documentId);
		this.assistantDocuments = this.availableDocuments.filter(doc => this.assistant.documentIds.find(id => id === doc.documentId) != undefined);
		this._availableDocuments.push(doc);
	}

	addTool(tool: MintyTool) {
		if (this.assistant.tools.find(el => el === tool.name)) {
			return;
		}
		this.assistant.tools.push(tool.name);
		this.assistantTools.push(tool);
		this.tools = this.tools.filter(tool => this.assistantTools.find(asstTool => asstTool === tool) == undefined);
		// New object for better chances at sane change detection.
		this.assistant.tools = [...this.assistant.tools];
	}

	removeTool(tool: MintyTool) {
		this.assistant.tools = this.assistant.tools.filter(el => el !== tool.name);
		this.assistantTools = this.tools.filter(tool => this.assistant.tools.find(id => id === tool.name) != undefined);
		this.tools.push(tool);
	}

	writeValue(obj: any): void {
		this.assistant = obj;
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
