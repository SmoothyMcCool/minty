import { CommonModule } from '@angular/common';
import { Component, EventEmitter, forwardRef, Input, OnInit, Output } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AttributeMapEditorComponent } from './attribute-map-editor.component';
import { TaskSpecification, TaskRequest, AttributeMap, OutputTaskSpecification } from '../../../model/workflow/task-specification';
import { WorkflowService } from '../../workflow.service';

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
export class TaskEditorComponent implements ControlValueAccessor {

	@Input() name!: string;

	@Output() taskNameChanged = new EventEmitter<{ oldName: string, newName: string }>();

	task: TaskRequest | undefined = undefined;
	isOutputTask = false;
	taskDescription: string | undefined = undefined;
	taskSpecification: TaskSpecification | OutputTaskSpecification| undefined = undefined;
	taskNames: string[] = [];

	onChange = (_: any) => { };
	onTouched: any = () => {};

	constructor(private workflowService: WorkflowService) {
	}

	get configForEditor() {
		return this.task?.configuration ?? {};
	}

	showTaskDescription() {
		if (this.isOutputTask) {
			return;
		}
		if (this.taskDescription) {
			this.taskDescription = undefined;
			return;
		}

		if (!this.taskSpecification || !this.task) {
			return;
		}
		const taskSpec = this.taskSpecification as TaskSpecification;
		const description = taskSpec.description;
		const inputs = taskSpec.expects ? this.escapeHtml(taskSpec.expects) : 'No inputs';
		const outputs = taskSpec.produces ? this.escapeHtml(taskSpec.produces) : 'No outputs';
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

	changeTask(taskName: string) {
		this.task!.taskName = taskName;
		this.assignTask(this.task!);
		this.onChange(this.task);
		this.onTouched();
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
		this.assignTask(obj);
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

	private assignTask(task: TaskRequest) {
		if (this.task !== task) {
			this.task = {
				...task,
				configuration: task.configuration,
				layout: { ...task.layout }
			};
			this.taskSpecification = this.workflowService.getTaskSpecification(this.task!.taskName);
			this.taskNames = this.workflowService.listTaskNames();
			this.isOutputTask = false;
			if (!this.taskSpecification) {
				this.taskSpecification = this.workflowService.getOutputTaskSpecification(this.task!.taskName);
				this.taskNames = this.workflowService.listOutputTaskNames();
				this.isOutputTask = true;
			}
		}
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
