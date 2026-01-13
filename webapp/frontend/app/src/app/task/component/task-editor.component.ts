import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Popover } from 'bootstrap';
import { AttributeMap, TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { AttributeMapEditorComponent } from './task-configuration-editor.component';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { Model } from 'src/app/model/model';

@Component({
	selector: 'minty-task-editor',
	imports: [CommonModule, FormsModule, AttributeMapEditorComponent],
	templateUrl: 'task-editor.component.html',
	styleUrls: [],
	providers: [{
		provide: NG_VALUE_ACCESSOR,
		useExisting: forwardRef(() => TaskEditorComponent),
		multi: true
	}]
})
export class TaskEditorComponent implements OnInit, ControlValueAccessor, OnDestroy {

	@ViewChild('popoverButton', { static: false }) popoverButton !: ElementRef;

	@Input() name: string;
	private _taskSpecification: TaskSpecification ;
	@Input()
	set taskSpecification(value: TaskSpecification){
		this._taskSpecification = value;
		this.destroyPopover();
		this.createPopover();
	}
	get taskSpecification(): TaskSpecification {
		return this._taskSpecification;
	}
	@Input() defaults: string[];
	@Input() enumLists: EnumList[];
	@Input() models: Model[];

	@Output() taskNameChanged = new EventEmitter<{ oldName: string, newName: string }>();

	task: TaskRequest;

	popoverInstance !: Popover;

	onChange = (_: any) => { };
	onTouched: any = () => {};

	constructor() {
	}

	ngOnInit() {
	}

	get configForEditor() {
		return this.task?.configuration ?? {};
	}

	createPopover() {
		if (!this.taskSpecification || !this.task || !this.popoverButton) {
			return;
		}
		const inputs = this.taskSpecification.expects ? this.escapeHtml(this.taskSpecification.expects) : 'No inputs';
		const outputs = this.taskSpecification.produces ? this.escapeHtml(this.taskSpecification.produces) : 'No outputs';
		const html = `
<strong>Inputs:</strong> ${inputs}
<br>
<br>
<strong>Outputs:</strong> ${outputs}`;

		this.popoverInstance = new Popover(this.popoverButton.nativeElement, {
			content: html,
			html: true,
			trigger: 'click',
			placement: 'auto'
		});
	}

	escapeHtml(str: string): string {
		if (!str) {
			return '';
		}
		return str
			.replace(/&/g, '&amp;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&#039;');
	}

	ngOnDestroy(): void {
		this.destroyPopover();
	}

	destroyPopover() {
		if (this.popoverInstance) {
			this.popoverInstance.dispose();
		}
	}

	writeValue(obj: any): void {
		if (!obj) {
			return;
		}
		if (this.task !== obj) {
			this.task = {
				...obj,
				configuration: obj.configuration,
				layout: { ...obj.layout }
			};
			// Need a refresh for task to be valid.
			setTimeout(() => this.createPopover(), 0);
		}
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Ignored.
	}

	onStepNameChanged(name: string) {
		if (this.task.stepName === name) {
			return;
		}

		const updatedTask = { ...this.task, stepName: name };
		this.task = updatedTask;
		this.taskNameChanged.emit({ oldName: this.task.stepName, newName: name });
		this.onChange(this.task);
	}

	onConfigurationChanged(config: AttributeMap) {
		const same = Object.keys(config).every(k => this.task.configuration[k] === config[k]);

		if (!same) {
			this.task = {
				...this.task,
				configuration: { ...config }
			}
			this.onChange(this.task);
		}
	}
}
