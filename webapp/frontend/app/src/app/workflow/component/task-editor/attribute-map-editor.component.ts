import { Component, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MapEditorComponent } from './map-editor.component';
import { StringListEditorComponent } from './stringlist-editor.component';
import { EnumListEditorComponent } from './enumlist-editor.component';
import { PacketEditorComponent } from './packet-editor.component';
import { DocumentEditorComponent } from './document-editor.component';
import { AssistantConfigurationEditorComponent } from './assistant-configuration-editor.component';
import { PipelineTransformEditorComponent } from './pipeline-transform-editor.component';
import { WorkflowStateService } from '../workflow-editor/services/workflow-state.service';
import { EnumList } from '../../../model/workflow/enum-list';
import { TaskSpecification, AttributeMap } from '../../../model/workflow/task-specification';


@Component({
	selector: 'minty-attribute-map-editor',
	templateUrl: 'attribute-map-editor.component.html',
	imports: [CommonModule, FormsModule, MapEditorComponent, StringListEditorComponent, EnumListEditorComponent, PacketEditorComponent, DocumentEditorComponent, AssistantConfigurationEditorComponent, PipelineTransformEditorComponent],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => AttributeMapEditorComponent),
			multi: true
		}
	]
})
export class AttributeMapEditorComponent implements ControlValueAccessor {

	@Input() taskSpecification!: TaskSpecification;
	defaults: AttributeMap | undefined = undefined;
	enumLists: EnumList[] = [];
	initialized = false;

	config: AttributeMap = {};
	onChange = (_: any) => { };
	onTouched: any = () => {};

	resultTemplates: string[] = [];

	constructor(private workflowStateService: WorkflowStateService) {
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
		const ret = this.enumLists.find(el => el.name === param);
		if (!ret) {
			throw new Error('getChoicesFor: param ' + param + ' not found');
		}
		return ret;
	}

	writeValue(obj: any): void {
		if (!obj) {
			return;
		}
		this.enumLists = this.workflowStateService.enumLists;
		this.defaults = this.workflowStateService.defaults;

		this.config = { ...obj };
		this.initialized = true;
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