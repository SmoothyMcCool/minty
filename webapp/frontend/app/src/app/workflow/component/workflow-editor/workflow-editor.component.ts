import { CommonModule } from '@angular/common';
import { Component, ElementRef, forwardRef, HostListener, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { WorkflowGeometryService } from './services/workflow-geometry.service';
import { WorkflowNodePaletteComponent } from './workflow-node-palette.component';
import { WorkflowTaskEditorModalComponent } from './workflow-task-editor-modal.component';
import { WorkflowStateService } from './services/workflow-state.service';
import { WorkflowCanvasComponent } from './workflow-canvas.component';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { ConfirmationDialogComponent } from '../../../app/component/confirmation-dialog.component';
import { TaskRequest, TaskSpecification, OutputTaskSpecification } from '../../../model/workflow/task-specification';
import { Workflow } from '../../../model/workflow/workflow';
import { AutoResizeDirective } from '../../../pipe/auto-resize-directive';
import { FilterPipe } from '../../../pipe/filter-pipe';
import { TaskEditorComponent } from '../task-editor/task-editor.component';

@Component({
	selector: 'minty-workflow-editor',
	imports: [CommonModule, DragDropModule, FormsModule, TaskEditorComponent, WorkflowCanvasComponent, FilterPipe, ConfirmationDialogComponent, AutoResizeDirective, WorkflowNodePaletteComponent, WorkflowTaskEditorModalComponent],
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
export class WorkflowEditorComponent implements ControlValueAccessor {

	// For drawing and managing various click and drag functions
	@ViewChild('canvas', { static: true }) canvasRef!: ElementRef<HTMLDivElement>;

	onChange = (_: any) => { };
	onTouched: any = () => { };

	editTask: TaskRequest | undefined = undefined;
	workflow: Workflow | null = null;
	private changeFromWriteValue = false;

	pendingDeleteTask?: TaskRequest;

	addStepVisible = false;
	taskSearch = '';

	canvasMouse = { x: 0, y: 0 };
	spawnPosition = { x: 0, y: 0 };
	selectedIndex = 0;

	confirmDeleteStepVisible: boolean = false;

	constructor(private workflowStateService: WorkflowStateService,
		private workflowGeometryService: WorkflowGeometryService) {

		this.workflowStateService.workflow$.pipe(takeUntilDestroyed()).subscribe(workflow => {
			this.workflow = workflow;
			if (!this.changeFromWriteValue) {
				this.onTouched();
				this.onChange(workflow);
			}
		});
	}

	onNamedChanged(name: string) {
		this.workflowStateService.updateWorkflow(w => {
			w.name = name;
		});
	}

	onDescriptionChanged(description: string) {
		this.workflowStateService.updateWorkflow(w => {
			w.description = description;
		});
	}

	addStepAt(taskSpecification: TaskSpecification, x: number, y: number) {

		const updated = { ...taskSpecification.configuration };
		const defaults = this.workflowStateService.defaults;

		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				if (defaults && key in defaults) {
					updated[key] = defaults[key];
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

		this.workflowStateService.updateWorkflow(w => {
			w.steps.push(task);
		});
	}

	addStep(taskSpecification: TaskSpecification) {
		const spawn = this.spawnPosition || this.workflowGeometryService.getCanvasCentre(this.canvasRef.nativeElement);
		this.addStepAt(taskSpecification, spawn.x, spawn.y);
	}

	addOutputStep(taskSpecification: OutputTaskSpecification) {
		const spawn = this.spawnPosition || this.workflowGeometryService.getCanvasCentre(this.canvasRef.nativeElement);
		const updated = { ...taskSpecification.configuration };
		const defaults = this.workflowStateService.defaults
		if (taskSpecification.configuration) {
			for (const key of Object.keys(taskSpecification.configuration)) {
				if (defaults && key in defaults) {
					updated[key] = defaults[key];
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

		this.workflowStateService.updateWorkflow(w => {
			w.outputStep = task;
		});
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

	onDisplayTask(taskId: string) {
		let step: TaskRequest | undefined;
		const workflow = this.workflowStateService.workflow;

		if (!workflow) {
			return;
		}

		if (taskId === workflow.outputStep?.id) {
			step = this.cloneTask(workflow.outputStep);
		} else {
			step = this.cloneTask(workflow.steps.find(task => task.id === taskId));
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

	onMouseMove(location: { x: number, y: number }) {
		this.canvasMouse = location;
	}

	doneEditingStep() {
		if (!this.editTask) {
			console.error('doneEditingStep: editTask not set');
			return;
		}

		const updatedTask: TaskRequest = {
			...this.editTask,
			configuration: { ...this.editTask.configuration }
		};

		this.workflowStateService.updateWorkflow(w => {
			if (w.outputStep?.id === updatedTask.id) {
				w.outputStep = updatedTask;
			} else {
				w.steps = w.steps.map(step =>
					step.id === updatedTask.id ? updatedTask : step
				);
			}

			// If the number of inputs or outputs changed, we need to remove any invalid connections now.
			w.connections = this.workflowGeometryService.validateConnectionsForTask(this.editTask!, w.connections);

		});

		this.editTask = undefined;
	}

	deleteStep() {
		this.confirmDeleteStepVisible = true;
	}

	confirmDeleteStep() {
		if (!this.editTask) {
			console.error('confirmDeleteStep: editTask not set');
			return;
		}

		this.workflowStateService.updateWorkflow(w => {
			w.connections = w.connections.filter(conn => conn.readerId !== this.editTask!.id && conn.writerId != this.editTask!.id);
			w.steps = w.steps.filter(step => step.id !== this.editTask!.id)
			if (w.outputStep?.id === this.editTask!.id) {
				w.outputStep = undefined;
			}
		});

		this.confirmDeleteStepVisible = false;
		this.doneEditingStep();
	}

	specFor(task: TaskRequest): TaskSpecification | OutputTaskSpecification | undefined {
		let result = this.workflowStateService.taskSpecifications.find(spec => spec.taskName === task.taskName);
		if (!result) {
			return this.workflowStateService.outputTaskSpecifications.find(spec => spec.taskName === task.taskName);
		}
		return result;
	}

	writeValue(obj: any): void {
		this.changeFromWriteValue = true;
		const workflow = obj ? { ...obj, steps: [...obj.steps], connections: [...obj.connections] } : { steps: [], connections: [] };
		this.workflowStateService.setWorkflow(workflow);
		this.changeFromWriteValue = false;
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(_isDisabled: boolean): void {
		// Nah.
	}

	private cloneTask(task: TaskRequest | undefined): TaskRequest | undefined {
		if (!task) {
			return undefined;
		}

		return {
			...task,
			configuration: { ...task.configuration },
			layout: { ...task.layout }
		};
	}

	cancelEdit() {
		this.editTask = undefined;
	}

	onAddStepClicked() {
		this.spawnPosition = {
			x: this.canvasMouse.x,
			y: this.canvasMouse.y
		};

		this.addStepVisible = true;
	}

	filteredTaskSpecifications(): TaskSpecification[] {
		let results = this.workflowStateService.taskSpecifications;

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

	onTaskDeleteRequest(task: TaskRequest) {
		this.pendingDeleteTask = task;
		this.confirmDeleteStepVisible = true;
	}
}
