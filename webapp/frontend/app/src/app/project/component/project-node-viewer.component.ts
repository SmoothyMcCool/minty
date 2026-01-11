import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { MarkdownModule } from "ngx-markdown";
import { MermaidClipboardDirective } from "src/app/assistant/component/mermaid-clipboard.directive";

@Component({
	selector: 'minty-project-node-viewer',
	imports: [CommonModule, FormsModule, MarkdownModule, MermaidClipboardDirective],
	templateUrl: 'project-node-viewer.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => NodeViewerComponent),
			multi: true
		}
	]
})
export class NodeViewerComponent implements ControlValueAccessor {

	@Input() edit: boolean;

	text: string;

	onChange: any = () => { };
	onTouched: () => void = () => { };

	private debounceTimer: ReturnType<typeof setTimeout> | null = null;

	writeValue(value: string | null): void {
		this.text = value;
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

	onTextChange(text: string) {
		this.text = text;
		this.onTouched();

		if (this.debounceTimer) {
			clearTimeout(this.debounceTimer);
		}

		this.debounceTimer = setTimeout(() => {
			this.onChange(text);
		}, 500);
	}
}