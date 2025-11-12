import { CommonModule } from '@angular/common';
import { Component, ElementRef, forwardRef, Input, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { OutputTaskSpecification, TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { Workflow } from 'src/app/model/workflow/workflow';
import { TaskWidgetComponent } from './task-widget.component';
import { CdkDragMove, DragDropModule } from '@angular/cdk/drag-drop';
import { FilterPipe } from 'src/app/pipe/filter-pipe';
import { TaskEditorComponent } from 'src/app/task/component/task-editor.component';
import { WorkflowService } from '../workflow.service';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';

@Component({
	selector: 'minty-workflow-editor',
	imports: [CommonModule, DragDropModule, FormsModule, TaskEditorComponent, TaskWidgetComponent, FilterPipe, ConfirmationDialogComponent],
	templateUrl: 'workflow-editor.component.html',
	styleUrls: ['workflow.component.css', 'workflow-editor.component.css'],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => WorkflowEditorComponent),
			multi: true
		}
	]
})
export class WorkflowEditorComponent implements ControlValueAccessor, OnInit {

	private _taskSpecifications: TaskSpecification[] = [];
	@Input()
	set taskSpecifications(value: TaskSpecification[]){
		this._taskSpecifications = value;
		if (this._taskSpecifications) {
			this._taskSpecifications.forEach(spec => this.specGroups.push(spec.group))
			this.specGroups = [...new Set(this.specGroups)].sort((a, b) => a.localeCompare(b));
		}
	}
	get taskSpecifications(): TaskSpecification[] {
		return this._taskSpecifications;
	}
	@Input() outputTaskSpecifications: TaskSpecification[] = [];
	@Input() defaults: Map<string, string>;

	// For drawing and managing various click and drag functions
	@ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;
	@ViewChildren(TaskWidgetComponent) taskWidgets!: QueryList<TaskWidgetComponent>;
	hoveredPort: { task: TaskRequest; portIndex: number; isInput: boolean } | null = null;
	tempConnection: any = null;

	onChange: any = () => {};
	onTouched: any = () => {};

	workflow: Workflow;
	editTask: TaskRequest = undefined;
	specGroups: string[] = [];
	enumLists: EnumList[];

	confirmDeleteStepVisible: boolean = false;

	constructor(private workflowService: WorkflowService) {
	}

	ngOnInit() {
		this.workflowService.listEnumLists().subscribe(enumLists => {
			this.enumLists = enumLists;
		})
	}

	addStep(taskSpecification: TaskSpecification) {
		const config = taskSpecification.configuration;

		config.forEach((_value, key) => {
			// System and user defaults are stored in the form "Task Name::Property Name", so
			// we need to build that up to find our keys.
			const fullKey = taskSpecification.taskName + '::' + key;
			if (this.defaults?.has(fullKey)) {
				config.set(key, this.defaults.get(fullKey));
			}
		});

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			configuration: config,
			layout: {
				x: 0,
				y: 0,
				numInputs: taskSpecification.numInputs,
				numOutputs: taskSpecification.numOutputs
			}
		};

		this.workflow.steps.push(task);
	}

	addOutputStep(taskSpecification: OutputTaskSpecification) {
		const config = taskSpecification.configuration;

		config.forEach((_value, key) => {
			// System and user defaults are stored in the form "Task Name::Property Name", so
			// we need to build that up to find our keys.
			const fullKey = taskSpecification.taskName + '::' + key;
			if (this.defaults?.has(fullKey)) {
				config.set(key, this.defaults.get(fullKey));
			}
		});

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			configuration: config,
			layout: {
				x: 0,
				y: 0,
				numInputs: 0,
				numOutputs: 0
			}
		};

		this.workflow.outputStep = task;
	}

	onStartConnection(task: TaskRequest, data: { portIndex: number; isInput: boolean; event: MouseEvent }) {

		const widget = this.taskWidgets.find(taskWidget => taskWidget.task.id === task.id);
		const { x, y } = widget.getPortCenter(data.portIndex, data.isInput);

		// If there is already an existing connection here, remove it from the connections list and make it the "temp" connection.
		const conIndex = this.workflow.connections.findIndex(conn => (data.isInput && conn.readerId === task.id && conn.readerPort === data.portIndex) ||
			(!data.isInput && conn.writerId === task.id && conn.writerPort === data.portIndex));

		if (conIndex != -1) {
			const connection = this.workflow.connections.at(conIndex);
			const originatingStep = this.taskWidgets.find(w => (data.isInput ? connection.writerId : connection.readerId) === w.task.id);
			const ogPos: { x: number, y: number } = originatingStep.getPortCenter(data.portIndex, !data.isInput);
			this.workflow.connections.splice(conIndex, 1);

			// It's opposite-land. For this to work we have to build the tempConnection as though this were a new connection
			// built from the port the user didn't click on.
			this.tempConnection = {
				isInput: !data.isInput,
				portId: originatingStep.task.id,
				portIndex: !data.isInput ? connection.writerPort : connection.readerPort,
				toX: !data.isInput ? ogPos.x : x,
				toY: !data.isInput ? ogPos.y : y,
				fromX: !data.isInput ? x : ogPos.x,
				fromY: !data.isInput ? y : ogPos.y
			};

		} else {

			this.tempConnection = {
				isInput: data.isInput,
				portId: task.id,
				portIndex: data.portIndex,
				fromX: x,
				fromY: y,
				toX: x,
				toY: y
			};
		}

	}

	onEndConnection(toTask: TaskRequest, data: { portIndex: number; isInput: boolean; event: MouseEvent }) {
		if (this.tempConnection) {

			if (data.isInput && !this.tempConnection.isInput) {
				this.workflow.connections.push({
					readerId: toTask.id,
					readerPort: data.portIndex,
					writerId: this.tempConnection.portId,
					writerPort: this.tempConnection.portIndex
				});

			} else if (!data.isInput && this.tempConnection.isInput) {
				this.workflow.connections.push({
					readerId: this.tempConnection.portId,
					readerPort: this.tempConnection.portIndex,
					writerId: toTask.id,
					writerPort: data.portIndex
				});

			}

			this.tempConnection = null;
		}
	}

	onHoverPort(task: TaskRequest, data: { portIndex: number; isInput: boolean; entering: boolean }) {
		if (data.entering) {
			this.hoveredPort = { task: task, portIndex: data.portIndex, isInput: data.isInput };
		} else {
			this.hoveredPort = null;
		}
	}

	onDragMoved(event: CdkDragMove<HTMLElement>, task: TaskRequest) {
		const { x, y } = event.distance;
		task.layout.tempX = task.layout.x + x;
		task.layout.tempY = task.layout.y + y;
	}

	onDragEnded(event: any, task: TaskRequest) {
		const pos = event.source.getFreeDragPosition();
		task.layout.x = pos.x;
		task.layout.y = pos.y;
		task.layout.tempX = task.layout.x;
		task.layout.tempY = task.layout.y;
	}

	onMouseMove(event: MouseEvent) {
		if (this.tempConnection) {
			setTimeout(() => {
				const canvasRect = this.canvasRef.nativeElement.getBoundingClientRect();

				// Default: mouse position
				let toX = event.clientX - canvasRect.left;
				let toY = event.clientY - canvasRect.top;

				// If hovering over a port, snap to its center, but only if:
				// 1. it's a different task than the originating task.
				// 2. If the originating port not the same input/output type as the hovered task.
				if (this.hoveredPort && this.hoveredPort.task.id !== this.tempConnection.portId &&
					this.hoveredPort.isInput !== this.tempConnection.isInput) {
					toX = this.getPortCenterX(this.hoveredPort.task.id, this.hoveredPort.portIndex, this.hoveredPort.isInput);
					toY = this.getPortCenterY(this.hoveredPort.task.id, this.hoveredPort.portIndex, this.hoveredPort.isInput);
				}

				if (this.tempConnection.isInput) {
					this.tempConnection.fromX = toX;
					this.tempConnection.fromY = toY;
				} else {
					this.tempConnection.toX = toX;
					this.tempConnection.toY = toY;
				}
			}, 0);
		}
	}

	onDisplayTask(task: TaskRequest) {
		this.editTask = task;
	}

	doneEditingStep() {
		this.editTask = null;
	}

	deleteStep() {
		this.confirmDeleteStepVisible = true;
	}

	confirmDeleteStep() {
		this.workflow.connections = this.workflow.connections.filter(conn => conn.readerId !== this.editTask.id && conn.writerId != this.editTask.id);
		this.workflow.steps = this.workflow.steps.filter(step => step.id !== this.editTask.id)
		if (this.workflow.outputStep.id === this.editTask.id) {
			this.workflow.outputStep = null;
		}
		this.doneEditingStep();

	}

	onConnectorClicked(c) {
		console.log(JSON.stringify(c, undefined, 2));
	}

	cancelConnection() {
		this.tempConnection = null;
	}

	getPortCenterX(elementId: string, portIndex: number, isInput: boolean): number {
		const widget = this.taskWidgets.find(w => w.task.id === elementId);
		if (!widget) {
			return 0;
		}
		return widget.getPortCenter(portIndex, isInput).x;
	}

	getPortCenterY(elementId: string, portIndex: number, isInput: boolean): number {
		const widget = this.taskWidgets.find(w => w.task.id === elementId);
		if (!widget) {
			return 0;
		}
		return widget.getPortCenter(portIndex, isInput).y;
	}

	defaultsFor(task: TaskRequest): string[] {

		const result: string[] = [];

		task.configuration.forEach((_value, key) => {
			// System and user defaults are stored in the form "Task Name::Property Name", so
			// we need to build that up to find our keys.
			const fullKey = task.taskName + '::' + key;
			if (this.defaults?.has(fullKey)) {
				result.push(key);
			}
		});

		return result;
	}

	specFor(task: TaskRequest): TaskSpecification {
		let result = this._taskSpecifications.find(spec => spec.taskName === task.taskName);
		if (!result) {
			result = this.outputTaskSpecifications.find(spec => spec.taskName === task.taskName);
		}
		return result;
	}

	onStepNameChanged(data: {oldId: string, newId: string}) {
		this.workflow.connections.forEach(connection => {
			if (connection.readerId === data.oldId) {
				connection.readerId = data.newId;
			}
			if (connection.writerId === data.oldId) {
				connection.writerId = data.newId;
			}
		});
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

	getEscapedGroupId(group: string): string {
		return group.replace(/[^a-zA-Z0-9_-]/g, '-');
	}

	sortandFilterSpecifications(group: string, specifications: TaskSpecification[]) {
		if (!specifications) {
			return;
		}
		if (!group) {
			return specifications.sort((left, right) => left.taskName.localeCompare(right.taskName));
		}
		return specifications.filter(spec => spec.group === group).sort((left, right) => left.taskName.localeCompare(right.taskName));
	}
}
