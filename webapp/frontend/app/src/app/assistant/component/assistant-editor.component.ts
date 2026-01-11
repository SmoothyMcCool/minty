import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { MintyTool } from 'src/app/model/minty-tool';
import { Model } from 'src/app/model/model';
import { SliderComponent } from './slider.component';

@Component({
	selector: 'minty-assistant-editor',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, SliderComponent],
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

	@Input() models: Model[] = [];
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
	minContext: number;
	maxContext: number;

	assistantTools: MintyTool[] = [];

	onChange: any = () => { };
	onTouched: any = () => { };

	constructor() {
	}

	modelChanged(model: string) {
		this.assistant.model = model;
		this.assistant.documentIds = [];
		this.minContext = this.models.find(m => m.name === model)?.defaultContext;
		this.maxContext = this.models.find(m => m.name === model)?.maximumContext;
		if (this.assistant.contextSize < this.minContext) {
			this.assistant.contextSize = this.minContext;
		} else if (this.assistant.contextSize > this.maxContext) {
			this.assistant.contextSize = this.maxContext;
		}
	}

	addDoc(doc: MintyDoc) {
		if (this.assistant.documentIds.find(el => el === doc.documentId)) {
			return;
		}
		this.assistant.documentIds.push(doc.documentId);
		this.assistantDocuments.push(doc);
		this._documents = this._documents.filter(d => d.documentId !== doc.documentId);
		// New object for better chances at sane change detection.
		this.assistantDocuments = [...this.assistantDocuments];
	}

	removeDoc(doc: MintyDoc) {
		this.assistant.documentIds = this.assistant.documentIds.filter(el => el !== doc.documentId);
		this.assistantDocuments = this.assistantDocuments.filter(doc => this.assistant.documentIds.findIndex(id => id === doc.documentId) !== -1);
		this._documents.push(doc);
		this._documents = [...this._documents];
	}

	addTool(tool: MintyTool) {
		if (this.assistant.tools.find(el => el === tool.name)) {
			return;
		}
		this.assistant.tools.push(tool.name);
		this.assistantTools.push(tool);
		this._tools = this._tools.filter(t => t.name !== tool.name);
		// New object for better chances at sane change detection.
		this.assistantTools = [...this.assistantTools];
	}

	removeTool(tool: MintyTool) {
		this.assistant.tools = this.assistant.tools.filter(el => el !== tool.name);
		this.assistantTools = this.assistantTools.filter(tool => this.assistant.tools.findIndex(at => at.localeCompare(tool.name) === 0) !== -1);
		this._tools.push(tool);
		this._tools = [...this._tools];
	}

	writeValue(obj: any): void {
		if (obj == null) {
			return;
		}
		this.assistant = obj;
		this.modelChanged(this.assistant.model);
		this.tools = [...this.tools];
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
}
