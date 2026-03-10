import { CommonModule } from '@angular/common';
import { Component, ElementRef, forwardRef, HostListener, Input, OnInit, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Connection, OutputTaskSpecification, AttributeMap, TaskRequest, TaskSpecification } from 'src/app/model/workflow/task-specification';
import { Workflow } from 'src/app/model/workflow/workflow';
import { TaskWidgetComponent } from '../task-widget.component';
import { CdkDragMove, DragDropModule } from '@angular/cdk/drag-drop';
import { FilterPipe } from 'src/app/pipe/filter-pipe';
import { TaskEditorComponent } from 'src/app/task/component/task-editor.component';
import { WorkflowService } from '../../workflow.service';
import { EnumList } from 'src/app/model/workflow/enum-list';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { AssistantService } from 'src/app/assistant.service';
import { Model } from 'src/app/model/model';
import { DocumentService } from 'src/app/document.service';
import { MintyDoc } from 'src/app/model/minty-doc';
import { ToolService } from 'src/app/tool.service';
import { MintyTool } from 'src/app/model/minty-tool';
import { AutoResizeDirective } from 'src/app/pipe/auto-resize-directive';
import { WorkflowGeometryService } from './services/workflow-geometry.service';
import { WorkflowNodePaletteComponent } from './workflow-node-palette.component';
import { WorkflowTaskEditorModalComponent } from './workflow-task-editor-modal.component';

@Component({
	selector: 'minty-workflow-editor',
	imports: [CommonModule, DragDropModule, FormsModule, TaskEditorComponent, TaskWidgetComponent, FilterPipe, ConfirmationDialogComponent, AutoResizeDirective, WorkflowNodePaletteComponent, WorkflowTaskEditorModalComponent],
	templateUrl: 'workflow-editor.component.html',
	styleUrls: ['../workflow.component.css', 'workflow-editor.component.css'],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => WorkflowEditorComponent),
			multi: true
		}
	]
})
export class WorkflowEditorComponent implements ControlValueAccessor, OnInit {

	@ViewChild('taskSearchBox') taskSearchBox!: ElementRef<HTMLInputElement>;

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
	_editTask: TaskRequest = undefined;
	get editTask(): TaskRequest {
		return this._editTask;
	}
	set editTask(request: TaskRequest) {
		this._editTask = request;
		this.onTouched();
		this.onChange(this.workflow);
	}
	specGroups: string[] = [];
	enumLists: EnumList[];
	models: Model[];
	documents: MintyDoc[];
	tools: MintyTool[];

	pendingDeleteTask?: TaskRequest;

	addStepVisible = false;
	taskSearch = '';

	canvasMouse = { x: 0, y: 0 };
	spawnPosition = { x: 0, y: 0 };
	selectedIndex = 0;

	confirmDeleteStepVisible: boolean = false;

	constructor(private workflowService: WorkflowService,
		private workflowGeometryService: WorkflowGeometryService,
		private assistantService: AssistantService,
		private toolService: ToolService,
		private documentService: DocumentService) {
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

	onNamedChanged(name: string) {
		this.workflow.name = name;
		this.onTouched();
		this.onChange(this.workflow);
	}

	onDescriptionChanged(description: string) {
		this.workflow.description = description;
		this.onTouched();
		this.onChange(this.workflow);
	}

	addStepAt(taskSpecification: TaskSpecification, x: number, y: number) {

		const updated = { ...taskSpecification.configuration };

		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				if (this.defaults && key in this.defaults) {
					updated[key] = this.defaults[key];
				}
			}
		}

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			loggingActive: true,
			configuration: updated,
			layout: {
				x: x,
				y: y,
				numInputs: taskSpecification.numInputs,
				numOutputs: taskSpecification.numOutputs
			}
		};

		this.workflow.steps.push(task);
		this.updateWorkflow();
	}

	addStep(taskSpecification: TaskSpecification) {
		const spawn = this.spawnPosition || this.workflowGeometryService.getCanvasCentre(this.canvasRef.nativeElement);
		this.addStepAt(taskSpecification, spawn.x, spawn.y);
	}

	addOutputStep(taskSpecification: OutputTaskSpecification) {
		const spawn = this.spawnPosition || this.workflowGeometryService.getCanvasCentre(this.canvasRef.nativeElement);
		const updated = { ...taskSpecification.configuration };
		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				if (this.defaults && key in this.defaults) {
					updated[key] = this.defaults[key];
				}
			}
		}
		taskSpecification.configuration = updated;

		const task: TaskRequest = {
			taskName: taskSpecification.taskName,
			stepName: taskSpecification.taskName,
			id: crypto.randomUUID(),
			loggingActive: true,
			configuration: taskSpecification.configuration,
			layout: {
				x: spawn.x,
				y: spawn.y,
				numInputs: 0,
				numOutputs: 0
			}
		};

		this.workflow.outputStep = task;
		this.updateWorkflow();
	}

	@HostListener('document:keydown.space', ['$event'])
	onSpacebar(event: KeyboardEvent) {

		if (this.editTask) return;

		const target = event.target as HTMLElement;

		if (target.tagName === 'INPUT' ||
			target.tagName === 'TEXTAREA' ||
			target.isContentEditable) {
			return;
		}

		event.preventDefault();

		this.spawnPosition = { ...this.canvasMouse };
		this.addStepVisible = true;
	}

	@HostListener('document:keydown.escape', ['$event'])
	onEscape() {
		this.addStepVisible = false;
	}

	@HostListener('document:keydown', ['$event'])
	onKeyDown(event: KeyboardEvent) {

		if (!this.addStepVisible) {
			return;
		}

		const results = this.filteredTaskSpecifications();

		if (!results.length) {
			return;
		}

		if (event.key === 'ArrowDown') {
			event.preventDefault();
			this.selectedIndex = (this.selectedIndex + 1) % results.length;
		}

		if (event.key === 'ArrowUp') {
			event.preventDefault();
			this.selectedIndex =
				(this.selectedIndex - 1 + results.length) % results.length;
		}

		if (event.key === 'Enter') {
			event.preventDefault();
			this.addStep(results[this.selectedIndex]);
		}

		if (event.key === 'Escape') {
			this.addStepVisible = false;
		}
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
			const ogPos: { x: number, y: number } = originatingStep.getPortCenter(data.isInput ? connection.writerPort : connection.readerPort, !data.isInput);
			this.workflow.connections.splice(conIndex, 1);

			// It's opposite-land. For this to work we have to build the tempConnection as though this were a new connection
			// built from the port the user didn't click on.
			this.tempConnection = {
				isInput: !data.isInput,
				portId: originatingStep.task.id,
				portIndex: data.isInput ? connection.writerPort : connection.readerPort,
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

		const canvas = this.canvasRef.nativeElement;
		// Track mouse position relative to canvas
		this.canvasMouse = this.workflowGeometryService.getCanvasRelativePosition(event, canvas);

		if (!this.tempConnection) {
			return;
		}

		setTimeout(() => {

			let toPoint = {
				x: this.canvasMouse.x,
				y: this.canvasMouse.y
			};

			// Snap to port if hovering over valid port
			const snap = this.workflowGeometryService.getSnappedPortPosition(
				this.hoveredPort,
				this.tempConnection,
				(id: string) => this.getWidget(id)
			);

			if (snap) {
				toPoint = snap;
			}

			// Update connection preview geometry
			if (this.tempConnection.isInput) {
				this.tempConnection.fromX = toPoint.x;
				this.tempConnection.fromY = toPoint.y;
			} else {
				this.tempConnection.toX = toPoint.x;
				this.tempConnection.toY = toPoint.y;
			}

		}, 0);
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
		this.workflow.connections = this.workflowGeometryService.validateConnectionsForTask(this.editTask, this.workflow.connections);

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

	private getWidget(taskId: string) {
		return this.taskWidgets.find(w => w.task.id === taskId);
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

	trackStepById(_index: number, step: TaskRequest) {
		return step.id;
	}

	getConnectionPoints(c: Connection) {
		const writerWidget = this.getWidget(c.writerId);
		const readerWidget = this.getWidget(c.readerId);

		return this.workflowGeometryService.getConnectionPath(
			c,
			(id, port, isInput) => {
				const widget = id === c.writerId ? writerWidget : readerWidget;
				return this.workflowGeometryService.getPortCenter(widget, port, isInput).x;
			},
			(id, port, isInput) => {
				const widget = id === c.writerId ? writerWidget : readerWidget;
				return this.workflowGeometryService.getPortCenter(widget, port, isInput).y;
			}
		);
	}


	private cloneTask(task: TaskRequest): TaskRequest {
		return {
			...task,
			configuration: { ...task.configuration },
			layout: { ...task.layout }
		};
	}

	cancelEdit() {
		this.editTask = null;
	}

	openAddStep() {
		this.spawnPosition = {
			x: this.canvasMouse.x,
			y: this.canvasMouse.y
		};

		this.addStepVisible = true;
	}

	private updateWorkflow() {
		this.onTouched();
		this.onChange(this.workflow);
	}

	filteredTaskSpecifications(): TaskSpecification[] {

		let results = this.taskSpecifications;

		if (this.taskSearch?.trim()) {
			const term = this.taskSearch.toLowerCase();

			results = results.filter(spec =>
				spec.taskName.toLowerCase().includes(term)
			);
		}

		// Sort by name
		return results.sort((a, b) =>
			a.taskName.localeCompare(b.taskName)
		);
	}

	onTaskNodeSelected(spec: TaskSpecification) {
		this.addStep(spec);
		this.addStepVisible = false;
	}

	onOutputNodeSelected(spec: OutputTaskSpecification) {
		this.addOutputStep(spec);
		this.addStepVisible = false;
	}

	updateTaskName(data: { oldId: string; newId: string }) {
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

	onTaskDeleteRequest(task: TaskRequest) {
		this.pendingDeleteTask = task;
		this.confirmDeleteStepVisible = true;
	}
}
