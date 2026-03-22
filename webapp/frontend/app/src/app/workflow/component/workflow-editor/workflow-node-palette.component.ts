import { CommonModule } from '@angular/common';
import { Component, ElementRef, EventEmitter, Input, OnChanges, Output, SimpleChanges, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TaskSpecification, OutputTaskSpecification } from '../../../model/workflow/task-specification';
import { PaletteNodeEntry } from 'src/app/model/workflow/palette-node-entry';
import { WorkflowStateService } from './services/workflow-state.service';

@Component({
	selector: 'minty-workflow-node-palette',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: './workflow-node-palette.component.html',
	styleUrls: ['./workflow-node-palette.component.css']
})
export class WorkflowNodePaletteComponent implements OnChanges {

	private _visible: boolean = false;
	@Input() get visible() {
		return this._visible;
	}
	set visible(visible: boolean) {
		this._visible = visible;
		this.taskSpecifications = this.workflowStateService.taskSpecifications;
		this.outputTaskSpecifications = this.workflowStateService.outputTaskSpecifications;
	}

	@ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;

	@Output() taskSelected = new EventEmitter<TaskSpecification>();
	@Output() outputTaskSelected = new EventEmitter<OutputTaskSpecification>();
	@Output() closed = new EventEmitter<void>();

	taskSpecifications: TaskSpecification[] = [];
	outputTaskSpecifications: OutputTaskSpecification[] = [];

	search = '';
	selectedIndexes: Record<string, number> = {};

	public constructor(private workflowStateService: WorkflowStateService) { }

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['visible']?.currentValue === true) {
			this.selectedIndexes = {};
			// Wait one tick for *ngIf to render the input before focusing
			setTimeout(() => this.searchInput?.nativeElement.focus(), 0);
		}
	}

	get combinedList() {

		const term = this.search?.trim().toLowerCase();

		const tasks = (this.taskSpecifications || [])
			.filter(t => !term || t.taskName.toLowerCase().includes(term))
			.sort((a, b) => a.taskName.localeCompare(b.taskName))
			.map(t => ({ type: 'task', item: t }));

		const outputs = (this.outputTaskSpecifications || [])
			.filter(o => !term || o.taskName.toLowerCase().includes(term))
			.sort((a, b) => a.taskName.localeCompare(b.taskName))
			.map(o => ({ type: 'output', item: o }));

		return [...tasks, ...outputs];
	}

	get groupedNodes() {

		const term = this.search?.trim().toLowerCase();

		const nodes: PaletteNodeEntry[] = [
			...(this.taskSpecifications || [])
				.filter(t =>
					!term || t.taskName.toLowerCase().includes(term)
				)
				.map((t): PaletteNodeEntry => ({
					type: 'task',
					item: t
				})),

			...(this.outputTaskSpecifications || [])
				.filter(o =>
					!term || o.taskName.toLowerCase().includes(term)
				)
				.map((o): PaletteNodeEntry => ({
					type: 'output',
					item: o
				}))
		];

		const grouped: Record<string, PaletteNodeEntry[]> = {};

		for (const node of nodes) {
			const group = (node.item as any).group || 'Output';

			if (!grouped[group]) {
				grouped[group] = [];
			}

			grouped[group].push(node);
		}

		return Object.entries(grouped)
			.map(([group, items]) => ({
				group,
				items: items.sort((a, b) =>
					a.item.taskName.localeCompare(b.item.taskName)
				)
			}))
			.sort((a, b) => {
				if (a.group === 'Output') {
					return 1;
				}
				if (b.group === 'Output') {
					return -1;
				}
				return a.group.localeCompare(b.group);
			});
	}


	onSearchChange() {
		this.selectedIndexes = {};
	}

	// ================================
	// Selection
	// ================================

	selectItem(entry: any, groupName?: string, index?: number) {

		if (groupName !== undefined && index !== undefined) {
			// Track selection per group
			this.selectedIndexes[groupName] = index;
		}

		if (entry.type === 'task') {
			this.taskSelected.emit(entry.item);
		}

		if (entry.type === 'output') {
			this.outputTaskSelected.emit(entry.item);
		}

		this.search = '';
		this.closed.emit();
	}


	onKeyDown(event: KeyboardEvent) {

		if (!this.visible) {
			return;
		}

		if (event.key === 'Escape') {
			this.closed.emit();
			return;
		}

		const groups = this.groupedNodes;
		if (!groups.length) return;

		// Flatten selection into global index space for keyboard nav
		const flatList = this.combinedList;
		if (!flatList.length) return;

		let currentFlatIndex = 0;

		// Find current selection position
		for (let g = 0; g < groups.length; g++) {

			const groupName = groups[g].group;
			const selectedIndex = this.selectedIndexes[groupName] ?? 0;

			if (selectedIndex < groups[g].items.length) {
				currentFlatIndex = this.getCombinedIndex(g, selectedIndex);
				break;
			}
		}

		if (event.key === 'ArrowDown') {
			event.preventDefault();

			currentFlatIndex =
				(currentFlatIndex + 1) % flatList.length;

			const node = flatList[currentFlatIndex];

			this.selectItem(node);
		}

		if (event.key === 'ArrowUp') {
			event.preventDefault();

			currentFlatIndex =
				(currentFlatIndex - 1 + flatList.length) % flatList.length;

			const node = flatList[currentFlatIndex];

			this.selectItem(node);
		}

		if (event.key === 'Enter') {
			event.preventDefault();

			const node = flatList[currentFlatIndex];
			this.selectItem(node);
		}
	}


	getGroupListWithNodes() {
		return this.groupedNodes;
	}

	private getCombinedIndex(groupIndex: number, itemIndex: number) {
		let offset = 0;

		for (let i = 0; i < groupIndex; i++) {
			offset += this.groupedNodes[i].items.length;
		}

		return offset + itemIndex;
	}

}
