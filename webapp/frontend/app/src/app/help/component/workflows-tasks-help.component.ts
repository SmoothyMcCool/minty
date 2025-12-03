import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WorkflowService } from 'src/app/workflow/workflow.service';


@Component({
	selector: 'minty-workflows-tasks-help',
	imports: [CommonModule],
	templateUrl: 'workflows-tasks-help.component.html',

})
export class WorkflowsTasksHelpComponent implements OnInit {

	taskHelpFiles: Map<string, string> = new Map();
	outputHelpFiles: Map<string, string> = new Map();
	content: string;

	constructor(private workflowService: WorkflowService) {
	}

	ngOnInit(): void {
		this.workflowService.getTaskHelpFiles().subscribe(entries => {
			this.taskHelpFiles = entries;
		});
		this.workflowService.getOutputHelpFiles().subscribe(entries => {
			this.outputHelpFiles = entries;
		});
	}

	compareKeys(left: { key: string, value: any}, right: { key: string, value: any }): number {
		return left.key.localeCompare(right.key);
	}

	displayTaskHelp(key: string) {
		this.content = this.taskHelpFiles.get(key);
	}

	displayOutputHelp(key: string) {
		this.content = this.outputHelpFiles.get(key);
	}
}
