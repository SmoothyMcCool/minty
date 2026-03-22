import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { EnumList } from 'src/app/model/workflow/enum-list';

@Component({
	selector: 'minty-enumlist-editor',
	templateUrl: 'enumlist-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => EnumListEditorComponent),
			multi: true
		}
	]
})
export class EnumListEditorComponent implements ControlValueAccessor {

	selection: string;
	onChange = (_: any) => { };
	onTouched: any = () => {};

	@Input() choices: EnumList;

	constructor() {
	}

	valueChanged(value: string) {
		this.selection = value;
		this.onChange(this.selection);
	}

	writeValue(obj: any): void {
		if (!obj) {
			return;
		}
		this.selection = obj as string;
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