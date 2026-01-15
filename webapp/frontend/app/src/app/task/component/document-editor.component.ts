import { CommonModule } from "@angular/common";
import { Component, forwardRef } from "@angular/core";
import { FormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor } from "@angular/forms";

@Component({
	selector: 'minty-document-editor',
	templateUrl: 'document-editor.component.html',
	imports: [CommonModule, FormsModule],
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => DocumentEditorComponent),
			multi: true
		}
	]
})
export class DocumentEditorComponent implements ControlValueAccessor {

	onChange = (_: any) => { };
	onTouched: any = () => { };

	fileSize = 0;
	fileName = '';

	private reader?: FileReader;

	fileListChanged(event: Event) {
		const fileList = (event.target as HTMLInputElement).files;
		if (fileList && fileList.length > 0) {
			const file = fileList[0];
			this.fileSize = file.size
			this.fileName = file.name;

			this.readFileAsBase64(file).then(base64 => {
				this.onChange(base64);
			});

			this.onTouched();
		}
	}

	private readFileAsBase64(file: File): Promise<string> {
		return new Promise((resolve, reject) => {
			this.reader?.abort();
			this.reader = new FileReader();

			this.reader.onerror = () => {
				this.reader.abort();
				reject(new Error('Problem parsing file'));
			};

			this.reader.onload = () => {
				const dataUrl = this.reader.result as string;
				const base64 = dataUrl.split(',')[1];
				resolve(base64);
			};

			this.reader.readAsDataURL(file);
		});
	}

	writeValue(_obj: any): void {
		// This component doesn't accept pre-existing input. Has to be set every time.
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

	ngOnDestroy(): void {
		this.reader?.abort();
	}
}