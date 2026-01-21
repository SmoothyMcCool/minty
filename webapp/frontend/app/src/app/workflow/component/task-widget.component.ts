import { Component, HostListener, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {TaskRequest } from 'src/app/model/workflow/task-specification';

@Component({
	selector: 'minty-task-widget',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: 'task-widget.component.html',
	styleUrls: ['task-widget.component.css'],
	host: { 'class': 'canvas-element' }
})
export class TaskWidgetComponent implements OnInit {

	_task: TaskRequest = null;
	@Input()
		set task(value: TaskRequest){
			this._task = value;
			this.recalc();
		}
		get task(): TaskRequest {
			return this._task;
		}
	@Output() startConnection = new EventEmitter<{ portIndex: number; isInput: boolean; event: MouseEvent }>();
	@Output() endConnection = new EventEmitter<{ portIndex: number; isInput: boolean; event: MouseEvent }>();
	@Output() hoverPort = new EventEmitter<{ portIndex: number; isInput: boolean; entering: boolean }>();
	@Output() displayTask = new EventEmitter<null>();

	rect = { x: 1, y: 1, width: 98, height: 48 };

	topPorts: { x: number; y: number; size: number }[] = [];
	bottomPorts: { x: number; y: number; size: number }[] = [];

	ngOnInit() {
		this.recalc();
	}

	@HostListener('window:resize')
	onResize() {
		this.recalc();
	}

	recalc() {
		const numTopPorts = Number(this.task.configuration['Number of Inputs'] ?? this.task.layout.numInputs);
		const numBottomPorts = Number(this.task.configuration['Number of Outputs'] ?? this.task.layout.numOutputs);

		this.topPorts = this.calculatePorts(numTopPorts, this.rect.y);
		this.bottomPorts = this.calculatePorts(numBottomPorts, this.rect.y + this.rect.height);
	}

	calculatePorts(n: number, y: number): { x: number; y: number; size: number }[] {
		const r = this.rect;
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

	getPortCenter(portIndex: number, isInput: boolean): { x: number; y: number } {
		const ports = isInput ? this.topPorts : this.bottomPorts;
		if (!ports[portIndex]) {
			console.warn(
				`TaskWidgetComponent.getPortCenter(): portIndex ${portIndex} out of range for task ${this.task.stepName}. ` +
				`Available ports: ${ports.length}`
			);
			return { x: 0, y: 0 };
		}
		// Account for task layout position
		const layoutX = this.task.layout.tempX ?? this.task.layout.x;
		const layoutY = this.task.layout.tempY ?? this.task.layout.y;
		return {
			x: layoutX + ports[portIndex].x + ports[portIndex].size / 2,
			y: layoutY + ports[portIndex].y + ports[portIndex].size / 2
		};
	}

	onPortMouseUp(portIndex: number, isInput: boolean, event: MouseEvent) {
		this.endConnection.emit({ portIndex, isInput, event });
	}

	onPortMouseDown(portIndex: number, isInput: boolean, event: MouseEvent) {
		this.startConnection.emit({ portIndex, isInput, event });
	}

	onTaskClick() {
		this.displayTask.emit();
	}
}