import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'minty-stringlist-editor',
	templateUrl: 'stringlist-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => StringListEditorComponent),
			multi: true
		}
	]
})
export class StringListEditorComponent implements ControlValueAccessor {

	entries: string[] = [];
	onChange: any = () => {};
	onTouched: any = () => {};

	constructor() {
	}

	valueChanged(index: number, value: string) {
		this.entries[index] = value;
		this.onChange(JSON.stringify(this.entries));
	}

	writeValue(obj: any): void {
		this.entries = [];
		if (!obj) {
			return;
		}
		this.entries = JSON.parse(obj);
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

	addEntry() {
		this.entries.push('');
		this.onChange(JSON.stringify(this.entries));
	}

	removeEntry(index: number) {
		this.entries.splice(index, 1);
		this.onChange(JSON.stringify(this.entries));
	}

	trackByIndex(index: number, _item: any): number {
		return index;
	}
}