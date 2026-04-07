import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkflowGeometryService } from './workflow-editor/services/workflow-geometry.service';
import { TaskRequest } from '../../model/workflow/task-specification';

@Component({
	selector: 'minty-task-widget',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: 'task-widget.component.html',
	styleUrls: ['task-widget.component.css'],
	host: { 'class': 'canvas-element' },
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskWidgetComponent implements OnChanges {

	@Input() task!: TaskRequest;
	private numInputs = 0;
	private numOutputs = 0;
	inputPorts: { x: number; y: number; size: number }[] = [];
	outputPorts: { x: number; y: number; size: number }[] = [];

	@Output() startConnection = new EventEmitter<{ portIndex: number; isInput: boolean; event: MouseEvent }>();
	@Output() endConnection = new EventEmitter<{ portIndex: number; isInput: boolean; event: MouseEvent }>();
	@Output() hoverPort = new EventEmitter<{ portIndex: number; isInput: boolean; entering: boolean }>();
	@Output() displayTask = new EventEmitter<null>();

	rect = WorkflowGeometryService.TaskRect;

	public constructor(private workflowGeometryService: WorkflowGeometryService) { }

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['task']) {
			this.refreshPorts();
		}
	}

	private refreshPorts(): void {
		const numInputPorts = Number(this.task.configuration['Number of Inputs'] ?? this.task.layout.numInputs);
		const numOutputports = Number(this.task.configuration['Number of Outputs'] ?? this.task.layout.numOutputs);

		if (numInputPorts !== this.numInputs) {
			this.numInputs = numInputPorts;
			this.inputPorts = this.workflowGeometryService.calculateInputPorts(this.task);
		}

		if (numOutputports !== this.numOutputs) {
			this.numOutputs = numOutputports;
			this.outputPorts = this.workflowGeometryService.calculateOutputPorts(this.task);
		}
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