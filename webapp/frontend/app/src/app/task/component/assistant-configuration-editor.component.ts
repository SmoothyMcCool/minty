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

	assistantSpec: AssistantSpec | null = null;
	useCustomAssistant: boolean = true;

	onChange = (_: any) => { };
	onTouched: () => void = () => { };

	assistantId: string;
	assistant: Assistant = createAssistant();

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
		this.onTouched();
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

	assistantChanged(assistant: Assistant) {
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
		const newSpec = this.useCustomAssistant
			? { assistantId: null, assistant: this.assistant }
			: { assistantId: this.assistantId, assistant: null };

		this.assistantSpec = newSpec;
		this.onTouched();
		this.onChange({ ...this.assistantSpec });
	}

}