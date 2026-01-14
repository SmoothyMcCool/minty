import { CommonModule } from '@angular/common';
import { Component, ElementRef, forwardRef, Input, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Connection, OutputTaskSpecification, AttributeMap, TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { Workflow } from 'src/app/model/workflow/workflow';
import { TaskWidgetComponent } from './task-widget.component';
import { CdkDragMove, DragDropModule } from '@angular/cdk/drag-drop';
import { FilterPipe } from 'src/app/pipe/filter-pipe';
import { TaskEditorComponent } from 'src/app/task/component/task-editor.component';
import { WorkflowService } from '../workflow.service';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { AssistantService } from 'src/app/assistant.service';
import { Model } from 'src/app/model/model';
import { DocumentService } from 'src/app/document.service';
import { MintyDoc } from 'src/app/model/minty-doc';
import { ToolService } from 'src/app/tool.service';
import { MintyTool } from 'src/app/model/minty-tool';

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
	set taskSpecifications(value: TaskSpecification[]) {
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
	@Input() defaults: AttributeMap;

	// For drawing and managing various click and drag functions
	@ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;
	@ViewChildren(TaskWidgetComponent) taskWidgets!: QueryList<TaskWidgetComponent>;
	hoveredPort: { task: TaskRequest; portIndex: number; isInput: boolean } | null = null;
	tempConnection: any = null;

	onChange = (_: any) => { };
	onTouched: any = () => { };

	workflow: Workflow;
	editTask: TaskRequest = undefined;
	specGroups: string[] = [];
	enumLists: EnumList[];
	models: Model[];
	documents: MintyDoc[];
	tools: MintyTool[];

	confirmDeleteStepVisible: boolean = false;

	constructor(private workflowService: WorkflowService, private assistantService: AssistantService, private toolService: ToolService, private documentService: DocumentService) {
	}

	ngOnInit() {
		this.workflowService.listEnumLists().subscribe(enumLists => {
			this.enumLists = enumLists;
		});
		this.assistantService.models().subscribe(models => {
			this.models = models;
		});
		this.documentService.list().subscribe(documents => {
			this.documents = documents;
		});
		this.toolService.list().subscribe(tools => {
			this.tools = tools;
		})
	}

	addStep(taskSpecification: TaskSpecification) {

		const updated = { ...taskSpecification.configuration };

		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				// System and user defaults are stored in the form "Task Name::Property Name", so
				// we need to build that up to find our keys.
				const fullKey = taskSpecification.taskName + '::' + key;
				if (this.defaults && fullKey in this.defaults) {
					updated[key] = this.defaults[fullKey];
				}
			}
		}

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			configuration: updated,
			layout: {
				x: 0,
				y: 0,
				numInputs: taskSpecification.numInputs,
				numOutputs: taskSpecification.numOutputs
			}
		};

		this.workflow.steps.push(task);
		this.updateWorkflow();
	}

	addOutputStep(taskSpecification: OutputTaskSpecification) {
		const updated = { ...taskSpecification.configuration };
		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				// System and user defaults are stored in the form "Task Name::Property Name", so
				// we need to build that up to find our keys.
				const fullKey = taskSpecification.taskName + '::' + key;
				if (this.defaults &&fullKey in this.defaults) {
					updated[key] = this.defaults[fullKey];
				}
			}
		}
		taskSpecification.configuration = updated;

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			configuration: taskSpecification.configuration,
			layout: {
				x: 0,
				y: 0,
				numInputs: 0,
				numOutputs: 0
			}
		};

		this.workflow.outputStep = task;
		this.updateWorkflow();
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

		this.updateWorkflow();
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
		this.updateWorkflow();
	}

	onDragEnded(event: any, task: TaskRequest) {
		const pos = event.source.getFreeDragPosition();
		task.layout.x = pos.x;
		task.layout.y = pos.y;
		task.layout.tempX = task.layout.x;
		task.layout.tempY = task.layout.y;
		this.updateWorkflow();
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

	onDisplayTask(taskId: string) {
		let step: TaskRequest;

		if (taskId === this.workflow.outputStep?.id) {
			step = this.cloneTask(this.workflow.outputStep);
		} else {
			step = this.cloneTask(this.workflow.steps.find(task => task.id === taskId));
		}

		if (!step) {
			return;
		}

		this.editTask = {
			...step,
			configuration: { ...step.configuration },
			layout: { ...step.layout }
		};
	}

	doneEditingStep() {
		if (!this.editTask) {
			return;
		}

		const updatedTask: TaskRequest = {
			...this.editTask,
			configuration: { ...this.editTask.configuration }
		};

		if (this.workflow.outputStep?.id === updatedTask.id) {
			this.workflow.outputStep = updatedTask;
		} else {
			this.workflow.steps = this.workflow.steps.map(step =>
				step.id === updatedTask.id ? updatedTask : step
			);
		}

		// If the number of inputs or outputs changed, we need to remove any invalid connections now.
		this.workflow.connections = this.workflow.connections.filter(connection => {
			if (connection.readerId !== this.editTask.id && connection.writerId !== this.editTask.id) {
				return true;
			}
			const numInputPorts = this.editTask.configuration['Number of Inputs'] !== undefined
				? Number(this.editTask.configuration['Number of Inputs'])
				: this.editTask.layout.numInputs;

			const numOutputPorts = this.editTask.configuration['Number of Outputs'] !== undefined
				? Number(this.editTask.configuration['Number of Outputs'])
				: this.editTask.layout.numOutputs;

			if (connection.readerId === this.editTask.id) {
				return connection.readerPort < numInputPorts;
			}
			if (connection.writerId === this.editTask.id) {
				return connection.writerPort < numOutputPorts;
			}
			return true;
		});

		this.editTask = null;
		this.updateWorkflow();
	}

	deleteStep() {
		this.confirmDeleteStepVisible = true;
	}

	confirmDeleteStep() {
		this.workflow.connections = this.workflow.connections.filter(conn => conn.readerId !== this.editTask.id && conn.writerId != this.editTask.id);
		this.workflow.steps = this.workflow.steps.filter(step => step.id !== this.editTask.id)
		if (this.workflow.outputStep?.id === this.editTask.id) {
			this.workflow.outputStep = null;
		}
		this.confirmDeleteStepVisible = false;
		this.doneEditingStep();
		this.updateWorkflow();
	}

	onConnectorClicked(c: Connection) {
		console.log(JSON.stringify(c, undefined, 2));
	}

	cancelConnection() {
		this.tempConnection = null;
	}

	getTaskWidgetBoundingBox(elementId: string): { x: number, y: number, width: number, height: number } {
		const widget = this.taskWidgets.find(w => w.task.id === elementId);
		const step = this.workflow.steps.find(step => step.id === elementId);
		return { x: step.layout.x, y: step.layout.y, width: widget.rect.width, height: widget.rect.height };
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

		const result: string[] = Object.keys(task.configuration || {}).filter(key => this.defaults && key in this.defaults);
		return result;
	}

	specFor(task: TaskRequest): TaskSpecification {
		let result = this._taskSpecifications.find(spec => spec.taskName === task.taskName);
		if (!result) {
			result = this.outputTaskSpecifications.find(spec => spec.taskName === task.taskName);
		}
		return result;
	}

	onStepNameChanged(data: { oldId: string, newId: string }) {
		this.workflow.connections.forEach(connection => {
			if (connection.readerId === data.oldId) {
				connection.readerId = data.newId;
			}
			if (connection.writerId === data.oldId) {
				connection.writerId = data.newId;
			}
		});
		this.updateWorkflow();
	}

	writeValue(obj: any): void {
		this.workflow = obj ? { ...obj, steps: [...obj.steps], connections: [...obj.connections] } : { steps: [], connections: [] };
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

	trackStepById(_index: number, step: TaskRequest) {
		return step.id;
	}

	getConnectionPoints(c: Connection) {
		const writerX = this.getPortCenterX(c.writerId, c.writerPort, false);
		const writerY = this.getPortCenterY(c.writerId, c.writerPort, false);

		const readerX = this.getPortCenterX(c.readerId, c.readerPort, true);
		const readerY = this.getPortCenterY(c.readerId, c.readerPort, true);

		const offset = 20; // how far we dip below / go around

		// If this is a self-connection.
		if (c.writerId === c.readerId) {
			const box = this.getTaskWidgetBoundingBox(c.writerId);
			const rightX = box.x - offset;
			const downY = writerY + offset; // drop below the widget
			const topY = readerY - offset; // rise above reader

			return {
				d: `M ${writerX} ${writerY} ` + // start at bottom port
					`L ${writerX} ${downY} ` +  // drop
					`L ${rightX} ${downY} ` +   // horizontal around the right edge
					`L ${rightX} ${topY} ` +    // climb up to the top port
					`L ${readerX} ${topY}` +    // drop
					`L ${readerX} ${readerY}`,  // finish at reader port
				isStraight: false
			};
		}

		// If writer is above the reader, draw a straight line
		if (writerY < readerY) {
			return {
				d: `M ${writerX} ${writerY} L ${readerX} ${readerY}`,
				isStraight: true
			};
		}

		// Writer is higher, draw a U‑shaped poly‑line
		const midY = writerY + offset; // drop below writer
		const topY = readerY - offset; // rise above reader
		const readerWriterMidpoint = (readerX + writerX) / 2;

		return {
			d: `M ${writerX} ${writerY} ` +            // start
				`L ${writerX} ${midY} ` +              // drop
				`L ${readerWriterMidpoint} ${midY} ` + // horizontal
				`L ${readerWriterMidpoint} ${topY} ` + // rise
				`L ${readerX} ${topY} ` +              // horizontal
				`L ${readerX} ${readerY}`,             // finish
			isStraight: false
		};
	}

	private cloneTask(task: TaskRequest): TaskRequest {
		return {
			...task,
			configuration: { ...task.configuration },
			layout: { ...task.layout }
		};
	}

	private updateWorkflow() {
		this.onTouched();
		this.onChange(this.workflow);
	}
}
