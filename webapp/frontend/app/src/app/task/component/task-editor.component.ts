import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, forwardRef, Input, OnDestroy, OnInit, Output, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Popover } from 'bootstrap';
import { TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { TaskConfigurationEditorComponent } from './task-configuration-editor.component';
import { EnumList } from 'src/app/model/workflow/enum-list';

@Component({
	selector: 'minty-task-editor',
	imports: [CommonModule, FormsModule, TaskConfigurationEditorComponent],
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

	@Output() taskNameChanged = new EventEmitter<{ oldName: string, newName: string }>();

	task: TaskRequest;

	popoverInstance !: Popover;

	onChange: any = (_: any) => {};
	onTouched: any = () => {};

	constructor() {
	}

	ngOnInit() {
	}

	createPopover() {
		if (!this.taskSpecification || !this.task) {
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
			return "";
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
		this.task = obj;
		// Need a refresh for task to be valid.
		setTimeout(() => this.createPopover(), 0);
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

	onStepNameChanged($event: string) {
		this.taskNameChanged.emit({ oldName: this.task.stepName, newName: $event });
		this.task.stepName = $event;
	}
}
