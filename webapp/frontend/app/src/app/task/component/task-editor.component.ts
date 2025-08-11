import { CommonModule } from "@angular/common";
import { Component, ElementRef, forwardRef, Input, OnDestroy, OnInit, ViewChild } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { Task } from "../../model/task";
import { TaskConfigurationEditorComponent } from "./task-configuration-editor.component";
import { TaskDescription } from "src/app/model/task-description";
import { AssistantService } from "src/app/assistant.service";
import { Assistant } from "src/app/model/assistant";
import { Popover } from "bootstrap";

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

	assistants: Assistant[] = [];

	task: Task = {
		name: '',
		configuration: new Map<string, string>()
	};

	isFileTriggered: boolean = false;
	triggerDirectory: string = '';

	taskDescription: TaskDescription = {
		name: "",
		configuration: new Map<string, string>(),
		inputs: "",
		outputs: ""
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
			.replace(/&/g, "&amp;")
			.replace(/</g, "&lt;")
			.replace(/>/g, "&gt;")
			.replace(/"/g, "&quot;")
			.replace(/'/g, "&#039;");
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
		this.task.name = $event;
		this.taskDescription = this.taskTemplates.find(element => element.name === $event);
		this.destroyPopover();
		this.createPopover();
	}
}
