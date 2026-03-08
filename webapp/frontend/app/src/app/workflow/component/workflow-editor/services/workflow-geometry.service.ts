import { Injectable } from '@angular/core';
import { Connection, TaskRequest } from 'src/app/model/workflow/task-specification';
import { TaskWidgetComponent } from '../../task-widget.component';

@Injectable({
	providedIn: 'root'
})
export class WorkflowGeometryService {

	private portOffset = 20;

	/**
	 * Get port center position
	 */
	getPortCenter(
		widget: TaskWidgetComponent | undefined,
		portIndex: number,
		isInput: boolean
	) {
		if (!widget) {
			return { x: 0, y: 0 };
		}

		return widget.getPortCenter(portIndex, isInput);
	}

	/**
	 * Get bounding box for node
	 */
	getBoundingBox(
		widget: TaskWidgetComponent | undefined,
		step: TaskRequest | undefined
	) {
		if (!widget || !step) {
			return { x: 0, y: 0, width: 0, height: 0 };
		}

		return {
			x: step.layout.x,
			y: step.layout.y,
			width: widget.rect.width,
			height: widget.rect.height
		};
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

	getSnappedPortPosition(hoveredPort: { task: TaskRequest; portIndex: number; isInput: boolean }, tempConnection: any, widgetResolver: (taskId: string) => TaskWidgetComponent | undefined) {
		if (!hoveredPort || !tempConnection) {
			return null;
		}

		if (hoveredPort.task.id === tempConnection.portId || hoveredPort.isInput === tempConnection.isInput) {
			return null;
		}

		const widget = widgetResolver(hoveredPort.task.id);

		if (!widget) {
			return null;
		}

		return widget.getPortCenter(
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
