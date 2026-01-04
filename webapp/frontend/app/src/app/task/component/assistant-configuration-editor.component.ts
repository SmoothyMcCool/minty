import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input } from "@angular/core";
import { FormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";
import { AssistantEditorComponent } from "src/app/assistant/component/assistant-editor.component";
import { Assistant, createAssistant } from "src/app/model/assistant";
import { AssistantSpec } from "src/app/model/workflow/assistant-spec";
import { EnumList } from "src/app/model/workflow/enum-list";
import { EnumListEditorComponent } from "./enumlist-editor.component";
import { Model } from "src/app/model/model";

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
export class AssistantConfigurationEditorComponent implements ControlValueAccessor {

	@Input() choices: EnumList;
	@Input() models: Model[];

	assistantSpec: AssistantSpec;
	idMode: boolean = true;

	onChange: any = () => { };
	onTouched: () => void = () => { };

	assistantId: string;
	assistant: Assistant = createAssistant();

	writeValue(value: AssistantSpec | null): void {
		this.assistantSpec = value ?? null;
		if (!this.assistantSpec) {
			// reset to defaults
			this.idMode = true;
			this.assistantId = '';
			this.assistant = createAssistant();
			return;
		}

		if (this.assistantSpec.assistantId) {
			this.idMode = false;
			this.assistantId = this.assistantSpec.assistantId;
			this.assistant = createAssistant();
		} else {
			this.idMode = true;
			this.assistant = this.assistantSpec.assistant ?? createAssistant();
			this.assistantId = '';
		}
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

	idModeChanged(customAssistant: boolean) {
		this.idMode = customAssistant;
		this.updateValueAndNotify();
	}

	assistantChanged(assistant: Assistant) {
		this.assistant = assistant;
		this.updateValueAndNotify();
	}

	idChanged(assistantId: string) {
		this.assistantId = assistantId;
		this.updateValueAndNotify();
	}

	private updateValueAndNotify(): void {
		this.assistantSpec = this.idMode
			? { assistantId: null, assistant: this.assistant }
			: { assistantId: this.assistantId, assistant: null };

		// Notify the parent only *after* the value has been updated.
		this.onChange(this.assistantSpec);
	}
}