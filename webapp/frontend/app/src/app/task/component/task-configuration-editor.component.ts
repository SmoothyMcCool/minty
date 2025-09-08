import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant } from 'src/app/model/assistant';
import { MapEditorComponent } from './map-editor.component';
import { StringListEditorComponent } from './stringlist-editor.component';
import { WorkflowService } from 'src/app/workflow/workflow.service';
import { EnumListEditorComponent } from './enumlist-editor.component';
import { ActivatedRoute } from '@angular/router';

@Component({
	selector: 'minty-task-config-editor',
	templateUrl: 'task-configuration-editor.component.html',
	imports: [CommonModule, FormsModule, MapEditorComponent, StringListEditorComponent, EnumListEditorComponent],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => TaskConfigurationEditorComponent),
			multi: true
		}
	]
})
export class TaskConfigurationEditorComponent implements ControlValueAccessor, OnInit {

	config: Map<string, string> = new Map();
	onChange: any = () => {};
	onTouched: any = () => {};

	resultTemplates: string[] = [];

	@Input() assistants: Assistant[] = [];

	private _taskConfiguration: Map<string, string>;
	@Input()
	set taskConfiguration(value: Map<string, string>){
		this._taskConfiguration = value;
	}
	get taskConfiguration(): Map<string, string> {
		return this._taskConfiguration;
	}

	private cachedChoices: { [key: string]: string[] } = {};

	constructor(private route: ActivatedRoute,
		private workflowService: WorkflowService) {
	}

	ngOnInit(): void {
		this.route.params.subscribe(() => {
			this.workflowService.listResultTemplates().subscribe(resultTemplates => {
				this.resultTemplates = resultTemplates;
			});
		});
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

	getChoicesFor(param: string): string[] {
		// There is likely a non-hacky way to do this, but I'm taking the easy way out for now.
		if (!this.cachedChoices[param] || this.cachedChoices[param].length === 0) {
			if (param === 'Pug Template') {
				this.cachedChoices[param] = this.resultTemplates.filter(el => el.endsWith('.pug'));
			}
			// Add more choices here as needed...
			else {
				this.cachedChoices[param] = [];
			}
			
		}
		
		return this.cachedChoices[param];
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

}