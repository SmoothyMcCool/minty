import { Injectable } from '@angular/core';
import { TaskRequest, Connection } from '../../../../model/workflow/task-specification';

@Injectable({
	providedIn: 'root'
})
export class WorkflowGeometryService {

	private portOffset = 20;

	public static TaskRect = { x: 1, y: 1, width: 98, height: 48 };

	/**
	 * Get port center position
	 */
	getPortCenter(task: TaskRequest, portIndex: number, isInput: boolean): { x: number, y: number } {
		if (!task) {
			return { x: 0, y: 0 };
		}

		const ports = isInput ? this.calculateInputPorts(task) : this.calculateOutputPorts(task);
		if (!ports[portIndex]) {
			console.warn(
				`TaskWidgetComponent.getPortCenter(): portIndex ${portIndex} out of range for task ${task.stepName}. ` +
				`Available ports: ${ports.length}`
			);
			return { x: 0, y: 0 };
		}
		// Account for task layout position
		const layoutX = task.layout.tempX ?? task.layout.x;
		const layoutY = task.layout.tempY ?? task.layout.y;
		return {
			x: layoutX + ports[portIndex].x + ports[portIndex].size / 2,
			y: layoutY + ports[portIndex].y + ports[portIndex].size / 2
		};
	}

	calculateInputPorts(task: TaskRequest): { x: number; y: number; size: number }[] {
		const numTopPorts = Number(task.configuration['Number of Inputs'] ?? task.layout.numInputs);
		return this.calculatePorts(numTopPorts, WorkflowGeometryService.TaskRect.y);
	}

	calculateOutputPorts(task: TaskRequest): { x: number; y: number; size: number }[] {
		const numBottomPorts = Number(task.configuration['Number of Outputs'] ?? task.layout.numOutputs);
		return this.calculatePorts(numBottomPorts, WorkflowGeometryService.TaskRect.y + WorkflowGeometryService.TaskRect.height);
	}

	private calculatePorts(n: number, y: number): { x: number; y: number; size: number }[] {
		const r = WorkflowGeometryService.TaskRect;
		const gap = r.width / (n + 1);
		const desiredSize = r.height * 0.5;
		const squareSize = Math.min(desiredSize, gap * 0.9, 15);

		const topY = y - squareSize / 2;

		const ports: { x: number; y: number; size: number }[] = [];

		for (let i = 0; i < n; i++) {
			const cx = r.x + gap * (i + 1);
			const x = cx - squareSize / 2;
			ports.push({ x, y: topY, size: squareSize });
		}

		return ports;
	}

	/**
	 * Calculate connection path SVG
	 */
	getConnectionPath(
		connection: Connection,
		getPortX: (id: string, port: number, isInput: boolean) => number,
		getPortY: (id: string, port: number, isInput: boolean) => number
	) {

		const writerX = getPortX(connection.writerId, connection.writerPort, false);
		const writerY = getPortY(connection.writerId, connection.writerPort, false);

		const readerX = getPortX(connection.readerId, connection.readerPort, true);
		const readerY = getPortY(connection.readerId, connection.readerPort, true);

		const offset = this.portOffset;

		// Self connection
		if (connection.writerId === connection.readerId) {

			const midX = writerX - offset;
			const downY = writerY + offset;
			const topY = readerY - offset;

			return {
				d:
					`M ${writerX} ${writerY} ` +
					`L ${writerX} ${downY} ` +
					`L ${midX} ${downY} ` +
					`L ${midX} ${topY} ` +
					`L ${readerX} ${topY} ` +
					`L ${readerX} ${readerY}`,
				isStraight: false
			};
		}

		// Straight line if writer is above reader
		if (writerY < readerY) {
			return {
				d: `M ${writerX} ${writerY} L ${readerX} ${readerY}`,
				isStraight: true
			};
		}

		// Curved path
		const midY = writerY + offset;
		const topY = readerY - offset;
		const midX = (readerX + writerX) / 2;

		return {
			d:
				`M ${writerX} ${writerY} ` +
				`L ${writerX} ${midY} ` +
				`L ${midX} ${midY} ` +
				`L ${midX} ${topY} ` +
				`L ${readerX} ${topY} ` +
				`L ${readerX} ${readerY}`,
			isStraight: false
		};
	}

	getCanvasRelativePosition(event: MouseEvent, canvas: HTMLElement) {
		return {
			x: event.clientX - canvas.getBoundingClientRect().left + canvas.scrollLeft,
			y: event.clientY - canvas.getBoundingClientRect().top + canvas.scrollTop
		};
	}

	getCanvasCentre(canvas: HTMLElement) {
		return {
			x: canvas.scrollLeft + canvas.clientWidth / 2,
			y: canvas.scrollTop + canvas.clientHeight / 2
		};
	}

	getSnappedPortPosition(hoveredPort: { task: TaskRequest; portIndex: number; isInput: boolean } | null, tempConnection: any, taskResolver: (taskId: string) => TaskRequest | undefined) {
		if (!hoveredPort || !tempConnection) {
			return null;
		}

		if (hoveredPort.task.id === tempConnection.portId || hoveredPort.isInput === tempConnection.isInput) {
			return null;
		}

		const task = taskResolver(hoveredPort.task.id);

		if (!task) {
			return null;
		}

		return this.getPortCenter(
			hoveredPort.task,
			hoveredPort.portIndex,
			hoveredPort.isInput
		);
	}

	validateConnectionsForTask(task: TaskRequest, connections: Connection[]) {
		const numInputPorts =
			task.configuration['Number of Inputs'] !== undefined
				? Number(task.configuration['Number of Inputs'])
				: task.layout.numInputs;

		const numOutputPorts =
			task.configuration['Number of Outputs'] !== undefined
				? Number(task.configuration['Number of Outputs'])
				: task.layout.numOutputs;

		return connections.filter(connection => {

			if (connection.readerId !== task.id &&
				connection.writerId !== task.id) {
				return true;
			}

			if (connection.readerId === task.id) {
				return connection.readerPort < numInputPorts;
			}

			if (connection.writerId === task.id) {
				return connection.writerPort < numOutputPorts;
			}

			return true;
		});
	}

}
