import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MapEditorComponent } from './map-editor.component';
import { StringListEditorComponent } from './stringlist-editor.component';
import { EnumListEditorComponent } from './enumlist-editor.component';
import { TaskSpecification } from 'src/app/model/workflow/task-specification';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { PacketEditorComponent } from './packet-editor.component';
import { DocumentEditorComponent } from './document-editor.component';

@Component({
	selector: 'minty-task-config-editor',
	templateUrl: 'task-configuration-editor.component.html',
	imports: [CommonModule, FormsModule, MapEditorComponent, StringListEditorComponent, EnumListEditorComponent, PacketEditorComponent, DocumentEditorComponent],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => TaskConfigurationEditorComponent),
			multi: true
		}
	]
})
export class TaskConfigurationEditorComponent implements ControlValueAccessor {

	@Input() defaults: string[] = [];
	@Input() taskSpecification: TaskSpecification;
	@Input() enumLists: EnumList[];
	config: Map<string, string> = new Map();
	onChange: any = () => {};
	onTouched: any = () => {};

	resultTemplates: string[] = [];

	constructor() {
	}


	valueChanged(param: string, value: string) {
		this.config.set(param, value);
		this.onTouched();
		this.onChange(this.config);
	}

	getConfig(key: string) {
		if (this.config && this.config.get !== undefined) {
			return this.config.get(key);
		}
		return null;
	}

	getChoicesFor(param: string): EnumList {
		return this.enumLists.find(el => el.name === param);
	}

	writeValue(obj: any): void {
		this.config = obj;
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(_isDisabled: boolean): void {
		// Nah.
	}

}