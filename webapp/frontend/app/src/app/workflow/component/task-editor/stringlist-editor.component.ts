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
	onChange = (_: any) => { };
	onTouched: any = () => { };

	constructor() {
	}

	valueChanged(index: number, value: string) {
		const next = [...this.entries];
		next[index] = value;
		this.entries = next;

		this.onTouched();
		this.onChange(JSON.stringify(next));
	}

	writeValue(obj: any): void {
		this.entries = [];
		if (!obj) {
			this.entries = [];
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
		const next = [...this.entries, ''];
		this.entries = next;
		this.onTouched();
		this.onChange(JSON.stringify(next));
	}

	removeEntry(index: number) {
		const next = this.entries.filter((_, i) => i !== index);
		this.entries = next;
		this.onTouched();
		this.onChange(JSON.stringify(next));
	}

	trackByIndex(index: number, _item: any): number {
		return index;
	}
}