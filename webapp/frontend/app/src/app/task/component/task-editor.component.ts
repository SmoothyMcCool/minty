import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Task } from '../../model/task';
import { TaskConfigurationEditorComponent } from './task-configuration-editor.component';
import { TaskDescription } from 'src/app/model/task-description';
import { AssistantService } from 'src/app/assistant.service';
import { Assistant } from 'src/app/model/assistant';
import { Popover } from 'bootstrap';
import { Observable, of } from 'rxjs';

@Component({
	selector: 'minty-task-editor',
	imports: [CommonModule, FormsModule, TaskConfigurationEditorComponent],
	templateUrl: 'task-editor.component.html',
	styleUrls: ['task-editor.component.css'],
	providers: [{
		provide: NG_VALUE_ACCESSOR,
		useExisting: forwardRef(() => TaskEditorComponent),
		multi: true
	}]
})
export class TaskEditorComponent implements OnInit, ControlValueAccessor, OnDestroy {

	@ViewChild('popoverButton', { static: false }) popoverButton !: ElementRef;

	@Input() name: string;
	@Input() edit: boolean = false;

	private _taskTemplates: TaskDescription[] ;
	@Input()
	set taskTemplates(value: TaskDescription[]){
		this._taskTemplates = value;
		if (this.task?.name) {
			this.taskChanged(this.task.name);
		}
	}
	get taskTemplates(): TaskDescription[]  {
		return this._taskTemplates;
	}

	private _defaults: Map<string, string>;
	@Input()
	set defaults(value: Map<string, string>) {
		this._defaults = value;
		if (this.task?.name) {
			this.taskChanged(this.task.name);
		}
	}
	get defaults(): Map<string, string> {
		return this._defaults;
	}

	assistants: Assistant[] = [];

	task: Task = {
		name: '',
		configuration: new Map<string, string>()
	};
	defaultFields: string[] = [];

	isFileTriggered: boolean = false;
	triggerDirectory: string = '';

	taskDescription: TaskDescription = {
		name: '',
		configuration: new Map<string, string>(),
		inputs: '',
		outputs: ''
	};

	popoverInstance !: Popover;
	htmlContent = `
			`;

	onChange: any = (value: any) => {};
	onTouched: any = () => {};

	constructor(private assistantService: AssistantService) {
	}

	ngOnInit() {
		this.assistantService.list().subscribe(assistants => {
			this.assistants = assistants;
		});
	}

	createPopover() {
		if (!this.taskDescription) {
			return;
		}
		const inputs = this.escapeHtml(this.taskDescription.inputs);
		const outputs = this.escapeHtml(this.taskDescription.outputs);
		const html = `
	<strong>Inputs:</strong> ${inputs}
	<br>
	<br>
	<strong>Outputs:</strong> ${outputs}
	`;

		this.popoverInstance = new Popover(this.popoverButton.nativeElement, {
			content: html,
			html: true,
			trigger: 'click',
			placement: 'auto'
		});
	}

	escapeHtml(str: string): string {
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
		if (this.task?.name) {
			this.taskChanged(this.task.name);
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

	taskChanged($event) {
		this.taskDescription = this.taskTemplates.find(element => element.name === $event);
		this.task.name = $event;
		if (this.taskDescription) {
			this.taskDescription.configuration.forEach((_value, key) => {
				// System and user defaults are stored in the form "Task Name::Property Name", so
				// we need to build that up to find our keys.
				const fullKey = this.task.name + '::' + key;
				if (this.defaults && this.defaults.has(fullKey)) {
					this.task.configuration.set(key, this.defaults.get(fullKey));
				}
			});
		}

		this.defaultFields = [];
		this.getDefaultFields().subscribe(defaults => this.defaultFields);

		this.destroyPopover();
		this.createPopover();
	}

	getDefaultFields(): Observable<string[]> {
		if (this.defaultFields && this.defaultFields.length > 0) {
			return of(this.defaultFields);
		}
		this.defaultFields = [];

		if (this.taskDescription) {
			this.taskDescription.configuration.forEach((_value, key) => {
					// System and user defaults are stored in the form "Task Name::Property Name", so
					// we need to build that up to find our keys.
					const fullKey = this.task.name + '::' + key;
					if (this.defaults && this.defaults.has(fullKey)) {
						this.defaultFields.push(key);
					}
				}
			);
		}

		return of(this.defaultFields);
	}
}
