import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MapEditorComponent } from './map-editor.component';
import { StringListEditorComponent } from './stringlist-editor.component';
import { EnumListEditorComponent } from './enumlist-editor.component';
import { TaskConfiguration, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { PacketEditorComponent } from './packet-editor.component';
import { DocumentEditorComponent } from './document-editor.component';
import { AssistantConfigurationEditorComponent } from './assistant-configuration-editor.component';
import { Model } from 'src/app/model/model';

@Component({
	selector: 'minty-task-config-editor',
	templateUrl: 'task-configuration-editor.component.html',
	imports: [CommonModule, FormsModule, MapEditorComponent, StringListEditorComponent, EnumListEditorComponent, PacketEditorComponent, DocumentEditorComponent, AssistantConfigurationEditorComponent],
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
	@Input() models: Model[];

	config: TaskConfiguration = {};
	onChange = (_: any) => { };
	onTouched: any = () => {};

	resultTemplates: string[] = [];

	constructor() {
	}

	valueChanged(key: string, value: string) {
		if (this.config[key] === value) {
			return;
		}

		this.config = { ...this.config, [key]: value };
		this.onTouched();
		this.onChange(this.config); // propagate to TaskEditor
	}

	getConfig(key: string) {
		const val = this.config[key];
		if (!val) {
			return null;
		}
		return val;
	}

	getChoicesFor(param: string): EnumList {
		return this.enumLists.find(el => el.name === param);
	}

	writeValue(obj: any): void {
		if (!obj) {
			return;
		}

		this.config = { ...obj };
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