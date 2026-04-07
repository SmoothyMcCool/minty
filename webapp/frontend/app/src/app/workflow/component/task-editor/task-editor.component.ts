import { CommonModule } from '@angular/common';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AttributeMapEditorComponent } from './attribute-map-editor.component';
import { TaskSpecification, TaskRequest, AttributeMap } from '../../../model/workflow/task-specification';

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
export class TaskEditorComponent implements OnInit, ControlValueAccessor {

	@Input() name!: string;
	@Input() taskSpecification!: TaskSpecification;

	@Output() taskNameChanged = new EventEmitter<{ oldName: string, newName: string }>();

	task: TaskRequest | undefined = undefined;
	taskDescription: string | undefined = undefined;

	onChange = (_: any) => { };
	onTouched: any = () => {};

	constructor() {	}

	ngOnInit() {
	}

	get configForEditor() {
		return this.task?.configuration ?? {};
	}

	showTaskDescription() {
		if (this.taskDescription) {
			this.taskDescription = undefined;
			return;
		}

		if (!this.taskSpecification || !this.task) {
			return;
		}
		const description = this.taskSpecification.description;
		const inputs = this.taskSpecification.expects ? this.escapeHtml(this.taskSpecification.expects) : 'No inputs';
		const outputs = this.taskSpecification.produces ? this.escapeHtml(this.taskSpecification.produces) : 'No outputs';
		const html = `
<div class="card">
	<div class="card-body">
		<strong>Description:</strong>${description}
		<br>
		<strong>Inputs:</strong> ${inputs}
		<br>
		<strong>Outputs:</strong> ${outputs}
	</div>
</div>
`;
		this.taskDescription = html;

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
		if (this.task && this.task.stepName === name) {
			return;
		}

		const updatedTask: TaskRequest = { ...this.task!, stepName: name };
		this.task = updatedTask;
		this.taskNameChanged.emit({ oldName: this.task.stepName, newName: name });
		this.onChange(this.task);
		this.onTouched();
	}

	onConfigurationChanged(config: AttributeMap) {
		const same = Object.keys(config).every(k => this.task!.configuration[k] === config[k]);

		if (!same) {
			this.task = {
				...this.task!,
				configuration: { ...config }
			}
			this.onChange(this.task);
			this.onTouched();
		}
	}

	onLoggingActiveChange(active: boolean) {
		this.task = {
				...this.task!,
				loggingActive: active
		}
		this.onChange(this.task);
		this.onTouched();
	}
}
