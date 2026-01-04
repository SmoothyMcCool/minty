import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, createAssistant } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { DocumentService } from '../../document.service';
import { FilterPipe } from '../../pipe/filter-pipe';
import { MintyDoc } from 'src/app/model/minty-doc';
import { AssistantEditorComponent } from './assistant-editor.component';
import { MintyTool } from 'src/app/model/minty-tool';
import { ToolService } from 'src/app/tool.service';
import { Model } from 'src/app/model/model';

@Component({
	selector: 'minty-new-assistant',
	imports: [CommonModule, FormsModule, RouterModule, FilterPipe, AssistantEditorComponent],
	templateUrl: 'new-assistant.component.html'
})
export class NewAssistantComponent implements OnInit {

	documents: MintyDoc[] = [];
	tools: MintyTool[] = [];
	models: Model[] = [];
	assistant: Assistant = createAssistant();
	assistantDocuments: MintyDoc[] = [];

	constructor(
		private assistantService: AssistantService,
		private documentService: DocumentService,
		private toolService: ToolService,
		private router: Router) {
	}

	ngOnInit(): void {
		this.assistantService.models().subscribe((models: Model[]) => {
			this.models = models;
		});
		this.documentService.list().subscribe(docs => {
			this.documents = docs;
		});
		this.toolService.list().subscribe((tools: MintyTool[]) => {
			this.tools = tools;
		});
	}

	formInvalid(): boolean {
		return this.assistant.name.length === 0 || this.assistant.model.length === 0;
	}

	createAssistant() {
		this.assistantService.create(this.assistant).subscribe(() => {
			this.navigateTo('assistants');
		});
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

}
