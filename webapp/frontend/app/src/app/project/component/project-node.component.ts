import { CommonModule } from "@angular/common";
import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ProjectFileType, ProjectNode, ProjectNodeType } from "../../model/project/project-node";
import { nodeMatchesFilter } from "../../model/project/project-node-filter";
import { AlertService } from "../../alert.service";

@Component({
	selector: 'minty-project-node',
	standalone: true,
	imports: [CommonModule, FormsModule],
	templateUrl: 'project-node.component.html',
	styleUrl: 'project-node.component.css',
	changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProjectNodeComponent implements OnChanges {
	@Input() nodes: ProjectNode[] = [];
	@Input() node!: ProjectNode;
	@Input() filter: string = '';
	@Input() selected!: ProjectNode | null;

	@Output() nodeSelected = new EventEmitter<ProjectNode>();
	@Output() update = new EventEmitter<ProjectNode>();
	@Output() delete = new EventEmitter<ProjectNode>();

	// Computed once in ngOnChanges rather than as getters, so they only
	// recompute when an @Input actually changes (not on every unrelated
	// change-detection cycle, e.g. someone typing in an input elsewhere).
	children: ProjectNode[] = [];
	filteredChildren: ProjectNode[] = [];
	fileName: string = '';
	hasActiveFilter: boolean = false;
	displayExpanded: boolean = true;

	editNodeInfoVisible = false;
	editName: string | undefined = undefined;
	editNodeType: ProjectNodeType = 'File';
	editFileType: ProjectFileType = 'text';
	editParent: string | undefined = undefined;
	isExpanded = true;

	public constructor(private alertService: AlertService) { }

	ngOnChanges(changes: SimpleChanges): void {
		if (changes['node']) {
			this.fileName = this.getFileName(this.node.path);
		}
		if (changes['node'] || changes['nodes']) {
			this._updateChildren();
		}
		if (changes['filter'] || changes['node'] || changes['nodes']) {
			this._updateFilteredChildren();
		}
	}

	getParentPath(input: string): string {
		if (!input) return '';
		const normalized = (input.endsWith('/') ? input.slice(0, -1) : input).replace(/\\/g, '/');
		const lastSlash = normalized.lastIndexOf('/');
		return lastSlash > 0 ? normalized.substring(0, lastSlash) : '';
	}

	getFileName(input: string): string {
		if (!input) return "";

		// Normalize Windows backslashes to forward slashes
		const normalized = input.replace(/\\/g, '/');

		// Remove trailing slash if present
		const trimmed = normalized.endsWith('/') ? normalized.slice(0, -1) : normalized;

		const lastSlash = trimmed.lastIndexOf('/');

		const final = lastSlash >= 0
			? trimmed.substring(lastSlash + 1)
			: trimmed;

		return final ? final : '/';
	}

	editNodeInfo(event?: MouseEvent) {
		event?.stopPropagation();
		this.editName = this.getFileName(this.node.path);
		this.editFileType = this.node.fileType || 'text';
		this.editNodeType = this.node.type;
		this.editParent = this.getParentPath(this.node.path);
		this.editNodeInfoVisible = true;
	}

	onConfirmNodeInfo() {
		const nodeInfo: ProjectNode = {
			type: this.editNodeType as any,
			path: (this.editParent ? this.editParent + '/' : '/') + this.editName,
			version: this.node.version + 1,
			fileType: this.editFileType as any,
			content: this.node.content
		};
		this.update.emit(nodeInfo);
		this.editNodeInfoVisible = false;
	}

	onCancelEditNodeInfo() {
		this.editNodeInfoVisible = false;
	}

	onSelect(node: ProjectNode) {
		if (this.node.path === node.path && this.node.type === 'Folder') {
			this.toggle();
		}
		this.nodeSelected.emit(node);
	}

	deleteNode() {
		if (this.node.path === '/') {
			this.alertService.postAlert({ type: 'failure', message: 'You can\'t delete the root node.' });
		} else {
			this.delete.emit(this.node);
		}
	}

	childNodesOf(parent: string): ProjectNode[] {
		const parentPath = parent.endsWith('/')
			? parent
			: parent + '/';

		return this.nodes.filter(node => {
			if (node.path === parent) {
				return false;
			}
			const parentParent = node.path.substring(0, node.path.lastIndexOf('/'));
			return parentParent === parent;
		});
	}

	toggle() {
		this.isExpanded = !this.isExpanded;
		this.displayExpanded = this.hasActiveFilter ? true : this.isExpanded;
	}

	listFolders(): ProjectNode[] {
		return this.nodes.filter(node => node.type === 'Folder' && node.path !== '/');
	}

	stopTreePropagation(event: KeyboardEvent) {
		event.stopPropagation();
	}

	private _updateChildren(): void {
		if (!this.node || !this.nodes) {
			this.children = [];
			return;
		}
		this.children = this.nodes.filter(n =>
			n.path !== this.node.path &&
			n.path.startsWith(this.node.path + '/') &&
			n.path.split('/').length === this.node.path.split('/').length + 1
		);
	}

	private _updateFilteredChildren(): void {
		this.hasActiveFilter = !!(this.filter ?? '').toString().trim();
		this.displayExpanded = this.hasActiveFilter ? true : this.isExpanded;
		this.filteredChildren = this.hasActiveFilter
			? this.children.filter(child => nodeMatchesFilter(child, this.nodes, this.filter))
			: this.children;
	}
}
