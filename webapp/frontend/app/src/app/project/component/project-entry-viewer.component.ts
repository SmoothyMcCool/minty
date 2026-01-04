import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { MarkdownModule } from "ngx-markdown";

@Component({
	selector: 'minty-project-entry-viewer',
	imports: [CommonModule, FormsModule, MarkdownModule],
	templateUrl: 'project-entry-viewer.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => ProjectEntryViewerComponent),
			multi: true
		}
	]
})
export class ProjectEntryViewerComponent implements ControlValueAccessor {

	@Input() edit: boolean;

	text: string;

	onChange: any = () => { };
	onTouched: () => void = () => { };

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
		this.onChange(text);
	}
}