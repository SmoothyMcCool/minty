import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from '../../model/assistant';
import { RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';

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
	set availableDocuments(value: MintyDoc[]){
		this._availableDocuments = value;
		this.assistantDocuments = this._availableDocuments.filter(doc => this.assistant.documentIds.find(id => id === doc.documentId) != undefined);
	}
	get availableDocuments(): MintyDoc[] {
		return this._availableDocuments;
	}

	assistantDocuments: MintyDoc[] = [];
	assistant: Assistant;

	onChange: any = () => {};
	onTouched: any = () => {};

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
		// New object for better chances at sane change detection.
		this.assistant.documentIds = [...this.assistant.documentIds];
	}

	removeDoc(doc: MintyDoc) {
		this.assistant.documentIds =
			this.assistant.documentIds.filter(el => el !== doc.documentId);
		this.assistantDocuments =
			this.availableDocuments.filter(doc =>
				this.assistant.documentIds.find(id =>
					id === doc.documentId) != undefined);
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
