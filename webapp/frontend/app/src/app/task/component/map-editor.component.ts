import { Component, forwardRef, Input } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Assistant } from "src/app/model/assistant";

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
	onChange: any = () => {};
	onTouched: any = () => {};

	mapAsString: string;

	constructor() {
	}

	keyChanged(index: number, key: string) {
		this.entries[index].key = key;
		this.mapAsString = this.entriesToString(this.entries);
		this.propagateChange();
	}

	valueChanged(index: number, value: string) {
		this.entries[index].value = value;
		this.mapAsString = this.entriesToString(this.entries);
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

	private stringToEntries(str): any[] {
		const result = [];
		const map = new Map(str.split(',').map(pair => pair.split(':')));
		map.forEach((value, key) => {
			result.push({ key: key, value: value });
		});
		return result;
	}

	writeValue(obj: any): void {
		this.entries = [];
		if (!obj) {
			return;
		}

		this.entries = this.stringToEntries(obj as string);
		this.mapAsString = obj;
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
		this.entries.push({ key: '', value: ''});
		this.propagateChange();
	}

	removeEntry(index: number) {
		this.entries.splice(index, 1);
		this.propagateChange();
	}

	private propagateChange() {
		this.onChange(this.mapAsString);
	}
}