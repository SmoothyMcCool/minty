import { CommonModule } from "@angular/common";
import { Component, Input, Output, EventEmitter } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { WorkflowStateService } from "./services/workflow-state.service";
import { ConfirmationDialogComponent } from "../../../app/component/confirmation-dialog.component";
import { MintyDoc } from "../../../model/minty-doc";
import { MintyTool } from "../../../model/minty-tool";
import { Model } from "../../../model/model";
import { EnumList } from "../../../model/workflow/enum-list";
import { TaskSpecification, AttributeMap, TaskRequest } from "../../../model/workflow/task-specification";
import { TaskEditorComponent } from "../task-editor/task-editor.component";

@Component({
	selector: 'minty-workflow-task-editor-modal',
	standalone: true,
	imports: [CommonModule, FormsModule, TaskEditorComponent, ConfirmationDialogComponent],
	templateUrl: './workflow-task-editor-modal.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: WorkflowTaskEditorModalComponent,
			multi: true
		}
	]
})
export class WorkflowTaskEditorModalComponent implements ControlValueAccessor {

	// -------- Inputs --------

	@Input() visible = false;
	@Input() specification?: TaskSpecification;

	enumLists: EnumList[] = [];
	models: Model[] = [];
	tools: MintyTool[] = [];
	documents: MintyDoc[] = [];
	defaults: AttributeMap | undefined = undefined;

	// -------- Outputs --------

	@Output() taskSaved = new EventEmitter<TaskRequest>();
	@Output() taskDeleted = new EventEmitter<TaskRequest>();
	@Output() cancel = new EventEmitter<void>();

	editedTask?: TaskRequest;

	onChange = (_: any) => {};
	onTouched = () => {}

	public constructor(private workflowStateService: WorkflowStateService) { }

	writeValue(obj: TaskRequest): void {
		if (obj) {
			this.editedTask = this.cloneTask(obj);
			this.enumLists = this.workflowStateService.enumLists;
			this.models = this.workflowStateService.models;
			this.tools = this.workflowStateService.tools;
			this.documents = this.workflowStateService.documents;
			this.defaults = this.workflowStateService.defaults;
		}
	}

	registerOnChange(fn: any): void { this.onChange = fn; }
	registerOnTouched(fn: any): void { this.onTouched = fn; }
	setDisabledState?(isDisabled: boolean): void { /* optional */ }

	done() {
		if (!this.editedTask) {
			return;
		}
		this.onTouched();
		this.onChange(this.editedTask);
		this.taskSaved.emit(this.editedTask);
	}

	cancelEdit() {
		this.cancel.emit();
	}

	deleteStep() {
		if (!this.editedTask) {
			return;
		}
		this.taskDeleted.emit(this.editedTask);
	}

	onTaskNameChanged(event: { oldId: string; newId: string }) {
		this.workflowStateService.updateWorkflow((w) => {
			w.connections.forEach(connection => {
				if (connection.readerId === event.oldId) {
					connection.readerId = event.newId;
				}
				if (connection.writerId === event.oldId) {
					connection.writerId = event.newId;
				}
			});
		});
	}

	private cloneTask(task: TaskRequest): TaskRequest {
		return {
			...task,
			configuration: { ...(task.configuration || {}) },
			layout: {
				x: task.layout?.x ?? 0,
				y: task.layout?.y ?? 0,
				numInputs: task.layout?.numInputs ?? 0,
				numOutputs: task.layout?.numOutputs ?? 0,
				tempX: task.layout?.tempX,
				tempY: task.layout?.tempY
			}
		};
	}
}
