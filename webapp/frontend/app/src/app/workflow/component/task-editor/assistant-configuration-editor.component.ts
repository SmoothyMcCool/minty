import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input, OnInit } from "@angular/core";
import { FormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";
import { EnumListEditorComponent } from "./enumlist-editor.component";
import { WorkflowStateService } from "../workflow-editor/services/workflow-state.service";
import { AssistantEditorComponent } from "../../../assistant/component/assistant-editor.component";
import { Assistant, createAssistant } from "../../../model/assistant";
import { MintyDoc } from "../../../model/minty-doc";
import { MintyTool } from "../../../model/minty-tool";
import { Model } from "../../../model/model";
import { AssistantSpec } from "../../../model/workflow/assistant-spec";
import { EnumList } from "../../../model/workflow/enum-list";

@Component({
	selector: 'minty-assistant-configuration-editor',
	templateUrl: 'assistant-configuration-editor.component.html',
	imports: [CommonModule, FormsModule, AssistantEditorComponent, EnumListEditorComponent],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => AssistantConfigurationEditorComponent),
			multi: true
		}
	]
})
export class AssistantConfigurationEditorComponent implements ControlValueAccessor, OnInit {

	@Input() choices: EnumList | undefined = undefined;
	models: Model[] = [];
	documents: MintyDoc[]  = [];
	tools: MintyTool[] = [];

	assistantSpec: AssistantSpec | undefined = undefined;
	useCustomAssistant: boolean = true;

	onChange = (_: any) => { };
	onTouched: () => void = () => { };

	assistantId: string | null = null;
	assistant: Assistant | undefined = undefined;

	public constructor(private workflowStateService: WorkflowStateService) {}

	ngOnInit() {
		this.models = this.workflowStateService.models;
		this.documents = this.workflowStateService.documents;
		this.tools = this.workflowStateService.tools;
	}

	writeValue(value: AssistantSpec | null): void {
		if (!value) {
			return;
		}

		const spec: AssistantSpec = typeof value === 'string' ? JSON.parse(value) : value;

		const sameSpec = this.assistantSpec &&
			this.assistantSpec.assistantId === spec.assistantId &&
			JSON.stringify(this.assistantSpec.assistant) === JSON.stringify(spec.assistant);

		if (sameSpec) {
			return;
		}

		this.assistantSpec = spec;

		if (spec.assistantId) {
			this.useCustomAssistant = false;
			this.assistantId = spec.assistantId;
			this.assistant = createAssistant();
		} else {
			this.useCustomAssistant = true;
			if (!this.assistant || JSON.stringify(this.assistant) !== JSON.stringify(spec.assistant)) {
				this.assistant = spec.assistant ?? createAssistant();
			}
			this.assistantId = '';
		}
		this.onChange({ ...this.assistantSpec });
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

	useCustomAssistantChanged(customAssistant: boolean) {
		if (this.useCustomAssistant === customAssistant) {
			return;
		}
		this.useCustomAssistant = customAssistant;
		this.updateValueAndNotify();
	}

	onAssistantEditorChange(assistant: Assistant) {
		if (JSON.stringify(this.assistant) === JSON.stringify(assistant)) {
			return;
		}
		this.assistant = assistant;
		this.updateValueAndNotify();
	}

	idChanged(assistantId: string) {
		if (this.assistantId === assistantId) {
			return;
		}
		this.assistantId = assistantId;
		this.updateValueAndNotify();
	}

	private updateValueAndNotify(): void {
		const newSpec: AssistantSpec = this.useCustomAssistant
			? { assistantId: null, assistant: this.assistant ? this.assistant : null }
			: { assistantId: this.assistantId, assistant: null };

		this.assistantSpec = newSpec;
		this.onTouched();
		this.onChange({ ...this.assistantSpec });
	}

}