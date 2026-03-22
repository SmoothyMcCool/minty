import { CdkDragMove, DragDropModule } from "@angular/cdk/drag-drop";
import { CommonModule } from "@angular/common";
import { Component, ElementRef, EventEmitter, forwardRef, Output, ViewChild } from "@angular/core";
import { NG_VALUE_ACCESSOR } from "@angular/forms";
import { TaskWidgetComponent } from "../task-widget.component";
import { WorkflowStateService } from "./services/workflow-state.service";
import { WorkflowGeometryService } from "./services/workflow-geometry.service";
import { Connection, TaskRequest } from "src/app/model/workflow/task-specification";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { Workflow } from "src/app/model/workflow/workflow";

@Component({
	selector: 'minty-canvas',
	imports: [CommonModule, DragDropModule, TaskWidgetComponent],
	templateUrl: 'workflow-canvas.component.html',
	styleUrls: ['./workflow-canvas.component.css'],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => WorkflowCanvasComponent),
			multi: true
		}
	]
})
export class WorkflowCanvasComponent {

	@Output() displayTask = new EventEmitter<string>();
	@Output() mouseMove = new EventEmitter<{ x: number, y: number }>();
	@Output() addStep = new EventEmitter<{ x: number, y: number }>();

	// For drawing and managing various click and drag functions
	@ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;

	tempConnection: any = null;
	hoveredPort: { task: TaskRequest; portIndex: number; isInput: boolean } | null = null;
	canvasMouse = { x: 0, y: 0 };
	workflow: Workflow | null = null;

	public constructor(private workflowStateService: WorkflowStateService,
		private workflowGeometryService: WorkflowGeometryService
	) {
		this.workflowStateService.workflow$.pipe(takeUntilDestroyed()).subscribe(workflow => {
			this.workflow = workflow;
		});
	}

	onStartConnection(task: TaskRequest, data: { portIndex: number; isInput: boolean; event: MouseEvent }) {
		const { x, y } = this.workflowGeometryService.getPortCenter(task, data.portIndex, data.isInput);
		const workflow = this.workflowStateService.workflow;

		// If there is already an existing connection here, remove it from the connections list and make it the "temp" connection.
		const conIndex = workflow.connections.findIndex(conn => (data.isInput && conn.readerId === task.id && conn.readerPort === data.portIndex) ||
			(!data.isInput && conn.writerId === task.id && conn.writerPort === data.portIndex));

		if (conIndex != -1) {
			const connection = workflow.connections.at(conIndex);
			const originatingStepId = data.isInput ? connection.writerId : connection.readerId;
			const originatingStep = this.workflowStateService.getTaskById(originatingStepId);
			const ogPos: { x: number, y: number } = this.workflowGeometryService.getPortCenter(originatingStep, data.isInput ? connection.writerPort : connection.readerPort, !data.isInput);
			this.workflowStateService.updateWorkflow(w => {
				w.connections.splice(conIndex, 1);
			});

			// It's opposite-land. For this to work we have to build the tempConnection as though this were a new connection
			// built from the port the user didn't click on.
			this.tempConnection = {
				isInput: !data.isInput,
				portId: originatingStep.id,
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
				this.workflowStateService.updateWorkflow(w => {
					w.connections.push({
						readerId: toTask.id,
						readerPort: data.portIndex,
						writerId: this.tempConnection.portId,
						writerPort: this.tempConnection.portIndex
					});
				});

			} else if (!data.isInput && this.tempConnection.isInput) {
				this.workflowStateService.updateWorkflow(w => {
					w.connections.push({
						readerId: this.tempConnection.portId,
						readerPort: this.tempConnection.portIndex,
						writerId: toTask.id,
						writerPort: data.portIndex
					});
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
		this.workflowStateService.updateWorkflow(_w => {
			task.layout.x = pos.x;
			task.layout.y = pos.y;
			task.layout.tempX = task.layout.x;
			task.layout.tempY = task.layout.y;
		});
	}

	onMouseMove(event: MouseEvent) {
		const canvas = this.canvasRef.nativeElement;
		// Track mouse position relative to canvas
		this.canvasMouse = this.workflowGeometryService.getCanvasRelativePosition(event, canvas);

		this.mouseMove.emit(this.canvasMouse);

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
				(id: string) => this.workflowStateService.getTaskById(id)
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
		this.displayTask.emit(taskId);
	}

	onAddStep() {
		this.addStep.emit();
	}

	cancelConnection() {
		this.tempConnection = null;
	}

	getConnectionPoints(c: Connection) {
		const writerTask = this.workflowStateService.getTaskById(c.writerId);
		const readerTask = this.workflowStateService.getTaskById(c.readerId);

		return this.workflowGeometryService.getConnectionPath(
			c,
			(id, port, isInput) => {
				const task = id === c.writerId ? writerTask : readerTask;
				return this.workflowGeometryService.getPortCenter(task, port, isInput).x;
			},
			(id, port, isInput) => {
				const task = id === c.writerId ? writerTask : readerTask;
				return this.workflowGeometryService.getPortCenter(task, port, isInput).y;
			}
		);
	}

	trackStepById(_index: number, step: TaskRequest) {
		return step.id;
	}

	get svgDimensions(): { width: number; height: number } {
		const steps = [
			...(this.workflow?.steps ?? []),
			...(this.workflow?.outputStep ? [this.workflow.outputStep] : [])
		];

		if (!steps.length) return { width: 2000, height: 2000 };

		const padding = 300; // Extra space beyond the furthest element
		const maxX = Math.max(...steps.map(s => (s.layout.tempX ?? s.layout.x) + 200));
		const maxY = Math.max(...steps.map(s => (s.layout.tempY ?? s.layout.y) + 150));

		return {
			width: Math.max(maxX + padding, 2000),
			height: Math.max(maxY + padding, 2000)
		};
	}

}