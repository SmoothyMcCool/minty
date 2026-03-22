import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'minty-map-editor',
	templateUrl: 'map-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => MapEditorComponent),
			multi: true
		}
	]
})
export class MapEditorComponent implements ControlValueAccessor {

	entries = [{ key: '', value: '' }];
	onChange = (_: any) => { };
	onTouched: any = () => { };

	mapAsString: string;

	constructor() {
	}

	keyChanged(index: number, key: string) {
		const next = [...this.entries];
		next[index] = { ...next[index], key };
		this.entries = next;
		this.propagateChange();
	}

	valueChanged(index: number, value: string) {
		const next = [...this.entries];
		next[index] = { ...next[index], value };
		this.entries = next;
		this.propagateChange();
	}

	private entriesToString(entries: any[]): string {
		let result = '';
		entries.forEach(entry => {
			result = ',' + result + entry.key + ':' + entry.value;
		});
		result = result.slice(1);
		return result;
	}

	writeValue(value: any): void {
		if (value === null || value === undefined) {
			this.entries = [{ key: '', value: '' }];
			this.mapAsString = '';
			return;
		}

		this.mapAsString = value;
		this.entries = value
			? value.split(',').map(pair => {
				const [k, v] = pair.split(':', 2);
				return { key: k ?? '', value: v ?? '' };
			})
			: [{ key: '', value: '' }];
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
		this.entries.push({ key: '', value: '' });
		this.propagateChange();
	}

	removeEntry(index: number) {
		this.entries.splice(index, 1);
		this.propagateChange();
	}

	private propagateChange() {
		const result = this.entries
			.filter(e => e.key)
			.map(e => `${e.key}:${e.value ?? ''}`)
			.join(',');

		this.mapAsString = result;
		this.onTouched();
		this.onChange(result);
	}
}