import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';

export interface DocProperties {
	title: string,
	file: File
}

@Component({
	selector: 'minty-document-editor',
	imports: [CommonModule, FormsModule],
	templateUrl: 'document-editor.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => DocumentEditorComponent),
			multi: true
		}
	]
})
export class DocumentEditorComponent implements ControlValueAccessor {

	document: DocProperties = {
		title: '',
		file: undefined
	};

	onChange = (_: any) => { };
	onTouched: any = () => {};

	constructor() {
	}

	onTitleChanged(title: string) {
		this.document = { ...this.document, title: title};
		this.onChange(this.document);
	}

	fileListChanged(event: Event) {
		const newFiles = (event.target as HTMLInputElement).files;
		if (newFiles && newFiles.length > 0) {
			this.document = { ...this.document, file: newFiles[0] };
			this.onChange(this.document);
		}
	}

	writeValue(obj: any): void {
		this.document = obj;
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
