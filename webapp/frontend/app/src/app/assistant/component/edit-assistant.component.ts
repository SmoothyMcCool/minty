import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Assistant, createAssistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FilterPipe } from '../../pipe/filter-pipe';
import { AssistantEditorComponent } from './assistant-editor.component';
import { MintyTool } from '../../model/minty-tool';
import { Model } from '../../model/model';
import { ToolService } from '../../tool.service';

@Component({
	selector: 'minty-edit-assistant',
	imports: [FormsModule, RouterModule, FilterPipe, AssistantEditorComponent],
	templateUrl: 'edit-assistant.component.html'
})
export class EditAssistantComponent implements OnInit {

	models: Model[] = [];
	tools: MintyTool[] = [];
	assistant: Assistant = createAssistant();

	constructor(
		private route: ActivatedRoute,
		private assistantService: AssistantService,
		private toolService: ToolService,
		private router: Router) {
	}

	ngOnInit(): void {
		this.route.params.subscribe(params => {
			this.assistantService.getAssistant(params['id']).subscribe((assistant: Assistant) => {
				this.assistant = assistant;
			});
		});
		this.assistantService.models().subscribe((models: Model[]) => {
			this.models = models;
		});
		this.toolService.list().subscribe(tools => this.tools = tools);
	}

	formInvalid(): boolean {
		return this.assistant.name.length === 0 || this.assistant.model.length === 0;
	}

	updateAssistant() {
		this.assistantService.update(this.assistant).subscribe(() => {
			this.navigateTo('assistants');
		});
	}

	navigateTo(url: string): void {
		this.router.navigate([url], { queryParamsHandling: 'merge' });
	}

}
