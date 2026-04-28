import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, createAssistant } from '../../model/assistant';
import { RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { SliderComponent } from './slider.component';
import { MintyDoc } from '../../model/minty-doc';
import { MintyTool } from '../../model/minty-tool';
import { Model } from '../../model/model';
import { AutoResizeDirective } from '../../pipe/auto-resize-directive';

@Component({
	selector: 'minty-assistant-editor',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, SliderComponent, AutoResizeDirective],
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
		if (value) {
			this._documents = value;
		}
		this.update();
	}
	get documents(): MintyDoc[] {
		return this._documents;
	}
	private _tools: MintyTool[] = [];
	@Input()
	set tools(value: MintyTool[]) {
		if (value) {
			this._tools = value;
		}
		this.update();
	}
	get tools(): MintyTool[] {
		return this._tools;
	}

	@Input() allowHistory: boolean = true;

	usedDocs: MintyDoc[] = [];
	unusedDocs: MintyDoc[] = [];
	assistant: Assistant | undefined = undefined;
	minContext: number = 0;
	maxContext: number = 0;

	usedTools: MintyTool[] = [];
	unusedTools: MintyTool[] = [];

	onChange = (_: any) => { };
	onTouched: any = () => { };

	constructor() {
	}

	update() {

		if (this.assistant) {
			this.modelChanged(this.assistant.model);
		}

		if (this.documents && this.assistant) {
			this.usedDocs = this.documents.filter(doc => this.assistant!.documentIds.find(id => id === doc.documentId) != undefined);
			this.unusedDocs = this.documents.filter(doc => this.usedDocs.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
		}

		if (this.tools && this.assistant) {
			this.usedTools = this.tools.filter(tool => this.assistant!.tools.find(name => name.localeCompare(tool.name) === 0) != undefined);
			this.unusedTools = this.tools.filter(tool => this.usedTools.find(asstTool => asstTool.name.localeCompare(tool.name) === 0) == undefined);
		}
	}

	modelChanged(modelName: string) {
		if (!this.models) {
			return;
		}

		if (this.assistant) {
			this.assistant = { ...this.assistant, model: modelName, documentIds: this.assistant.documentIds, contextSize: this.assistant.contextSize };


			const model = this.models.find(m => m.name === modelName);

			if (model) {
				this.minContext = 0;
				this.maxContext = model.maximumContext;

				if (this.assistant.contextSize < this.minContext) {
					this.assistant.contextSize = this.minContext;
				} else if (this.assistant.contextSize > this.maxContext) {
					this.assistant.contextSize = this.maxContext;
				}
			}
		}

		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	nameChanged(name: string) {
		if (!this.assistant) {
			return;
		}
		this.assistant.name = name;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	hasMemoryChanged(hasMemory: boolean) {
		if (!this.assistant) {
			return;
		}
		if (!this.allowHistory) {
			hasMemory = false;
		}
		this.assistant.hasMemory = hasMemory;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	contextSizeChanged(contextSize: number) {
		if (!this.assistant) {
			return;
		}
		this.assistant.contextSize = contextSize;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	temperatureChanged(temperature: number) {
		if (!this.assistant) {
			return;
		}
		this.assistant.temperature = temperature;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	topKChanged(topK: number) {
		if (!this.assistant) {
			return;
		}
		this.assistant.topK = topK;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	promptChanged(prompt: string) {
		if (!this.assistant) {
			return;
		}
		this.assistant.prompt = prompt;
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}
	addDoc(doc: MintyDoc) {
		if (!this.assistant) {
			return;
		}
		if (this.assistant.documentIds.find(el => el === doc.documentId)) {
			return;
		}
		this.assistant.documentIds.push(doc.documentId);
		this.usedDocs.push(doc);
		// New object for better chances at sane change detection.
		this.usedDocs = [...this.usedDocs];
		this.unusedDocs = this._documents.filter(doc => this.usedDocs.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}

	removeDoc(doc: MintyDoc) {
		if (!this.assistant) {
			return;
		}
		this.assistant.documentIds = this.assistant.documentIds.filter(el => el !== doc.documentId);
		this.usedDocs = this.usedDocs.filter(doc => this.assistant! .documentIds.findIndex(id => id === doc.documentId) !== -1);
		this.unusedDocs = this._documents.filter(doc => this.usedDocs.find(asstDoc => asstDoc.documentId === doc.documentId) == undefined);
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}

	addTool(tool: MintyTool) {
		if (!this.assistant) {
			return;
		}
		if (this.assistant.tools.find(el => el === tool.name)) {
			return;
		}
		this.assistant.tools.push(tool.name);
		this.usedTools.push(tool);
		// New object for better chances at sane change detection.
		this.usedTools = [...this.usedTools];
		this.unusedTools = this._tools.filter(tool => this.usedTools.find(asstTool => asstTool.name.localeCompare(tool.name) === 0) == undefined);
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}

	removeTool(tool: MintyTool) {
		if (!this.assistant) {
			return;
		}
		this.assistant.tools = this.assistant.tools.filter(el => el !== tool.name);
		this.usedTools = this.usedTools.filter(tool => this.assistant!.tools.findIndex(at => at.localeCompare(tool.name) === 0) !== -1);
		this.unusedTools = this._tools.filter(tool => this.usedTools.find(asstTool => asstTool.name.localeCompare(tool.name) === 0) == undefined);
		this.onTouched();
		this.onChange(createAssistant(this.assistant));
	}

	writeValue(obj: any): void {
		if (obj == null) {
			return;
		}
		this.assistant = createAssistant(obj);
		this.update();
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
