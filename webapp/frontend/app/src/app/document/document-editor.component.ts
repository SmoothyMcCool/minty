import { Component, forwardRef } from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DocumentSection, MintyDoc } from '../model/minty-doc';
import { DatePipe } from '@angular/common';
import { DocumentService } from '../document.service';
import { MarkdownModule } from 'ngx-markdown';


export interface DocProperties {
	title: string,
	file: File | undefined
}

@Component({
	selector: 'minty-document-editor',
	imports: [FormsModule, MarkdownModule, DatePipe],
	templateUrl: 'document-editor.component.html',
	providers: [
		{
			provide: NG_VALUE_ACCESSOR,
			useExisting: forwardRef(() => DocumentEditorComponent),
			multi: true
		}
	]
})
export class DocumentEditorComponent implements ControlValueAccessor {

	document: MintyDoc | undefined = undefined;
	selectedSection: DocumentSection | null = null;
	viewingMode: 'summary' | 'section' | 'none' = 'none';

	onChange = (_: any) => { };
	onTouched: any = () => {};

	constructor(private documentService: DocumentService) {
	}

	onTitleChanged(title: string) {
		this.document = { ...this.document!, title: title};
		this.onChange(this.document);
	}

	writeValue(obj: any): void {
		this.document = obj;
	}
	registerOnChange(fn: any): void {
		this.onChange = fn;
	}
	registerOnTouched(fn: any): void {
		this.onTouched = fn;
	}
	setDisabledState(isDisabled: boolean): void {
		// Nah.
	}

	getContent(section: DocumentSection) {
		this.documentService.getSectionContent(section.id).subscribe(content => {
			if (this.selectedSection) {
				this.selectedSection.content = content;
			}
		});
	}
}
