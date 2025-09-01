import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input, OnInit } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { Workflow } from "src/app/model/workflow/workflow";
import { TaskEditorComponent } from "src/app/task/component/task-editor.component";
import { TaskDescription } from "src/app/model/task-description";
import { UserService } from "src/app/user.service";
import { ActivatedRoute } from "@angular/router";

@Component({
	selector: 'minty-workflow-editor',
	imports: [CommonModule, FormsModule, TaskEditorComponent],
	templateUrl: 'workflow-editor.component.html',
	styleUrls: ['workflow.component.css'],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => WorkflowEditorComponent),
			multi: true
		}
	]
})
export class WorkflowEditorComponent implements ControlValueAccessor, OnInit {

	@Input() taskTemplates: TaskDescription[] = [];
	@Input() outputTaskTemplates: TaskDescription[] = [];
	@Input() edit: boolean = false;

	onChange: any = () => {};
	onTouched: any = () => {};

	workflow: Workflow = {
		name: '',
		description: '',
		id: '',
		ownerId: '',
		shared: false,
		workflowSteps: [],
		outputStep: {
			name: '',
			configuration: new Map()
		}
	};

	isFileTriggered: boolean = false;
	triggerDirectory: string = '';

	configParams = new Map<string, string>();
	outputTaskConfigParams = new Map<string, string>();

	defaults: Map<string, string>;

	constructor(private userService: UserService,
		private route: ActivatedRoute
	) {
	}

	ngOnInit() {
		this.route.params.subscribe(params => {
			this.userService.systemDefaults().subscribe(systemDefaults => {
				this.userService.userDefaults().subscribe(userDefaults => {
					// User defaults should take priority in conflicts.
					this.defaults = new Map([ ...systemDefaults, ...userDefaults ]);
				});
			});
		});
	}

	addStep(after: number) {
		const deleteCount = 0;
		this.workflow.workflowSteps.splice(after + 1, deleteCount, {
				name: '',
				configuration: new Map()
			});
	}

	addStepAtEnd() {
		this.workflow.workflowSteps.push({
				name: '',
				configuration: new Map()
			});
	}

	deleteStep(index: number) {
		this.workflow.workflowSteps.splice(index, 1);
	}

	getInputsFor(taskName: string): string {
		const task = this.taskTemplates.find(element => element.name === taskName);
		if (task) {
			return task.inputs;
		}
		return "";
	}

	getOutputsFor(taskName: string): string {
		const task = this.taskTemplates.find(element => element.name === taskName);
		if (task) {
			return task.outputs;
		}
		return "";
	}

	writeValue(obj: any): void {
		this.workflow = obj;
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
