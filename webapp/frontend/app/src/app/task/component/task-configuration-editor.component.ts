import { Component, forwardRef, Input } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { Assistant } from "src/app/model/assistant";
import { MapEditorComponent } from "./map-editor.component";
import { StringListEditorComponent } from "./stringlist-editor.component";

@Component({
	selector: 'minty-task-config-editor',
	templateUrl: 'task-configuration-editor.component.html',
	imports: [CommonModule, FormsModule, MapEditorComponent, StringListEditorComponent],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => TaskConfigurationEditorComponent),
			multi: true
		}
	]
})
export class TaskConfigurationEditorComponent implements ControlValueAccessor {

	config: Map<string, string> = new Map();
	onChange: any = () => {};
	onTouched: any = () => {};

	@Input() assistants: Assistant[] = [];

	private _taskConfiguration: Map<string, string>;
	@Input()
	set taskConfiguration(value: Map<string, string>){
		this._taskConfiguration = value;
	}
	get taskConfiguration(): Map<string, string> {
		return this._taskConfiguration;
	}

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

	writeValue(obj: any): void {
		this.config = obj;
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
		this.config.set('','');
	}

	removeEntry(key: string) {
		this.config.delete(key);
	}
}