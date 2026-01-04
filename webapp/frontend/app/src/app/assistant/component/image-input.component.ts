import { Component, EventEmitter, HostListener, Input, NgZone, Output } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'minty-image-input',
	imports: [CommonModule],
	templateUrl: 'image-input.component.html'
})
export class ImageInputComponent {

	@Input() imageSupport: boolean;

	@Input()
	get reset() {
		return false;
	}
	set reset(val: boolean) {
		if (val) {
			this.previewUrl = null;
		}
	}

	@Output() image = new EventEmitter<File>();
	previewUrl: string | ArrayBuffer | null = null

	constructor(private ngZone: NgZone) {
	}

	@HostListener('window:paste', ['$event'])
	onPaste(event: ClipboardEvent) {
		const file = this.getImageFromClipboard(event.clipboardData);
		if (!file) {
			// Nothing to do – let the browser paste text normally
			return;
		}

		event.preventDefault();

		this.readAndPreview(file);
		this.image.emit(file);
	}


	onDragOver(e: DragEvent) {
		e.preventDefault();
	}

	onDragLeave(e: DragEvent) {
		e.preventDefault();
	}

	onDrop(e: DragEvent) {
		e.preventDefault();

		const files = e.dataTransfer?.files;
		if (!files || files.length === 0) return;

		// Grab the first image file (ignore non‑image drops)
		const file = Array.from(files).find(f => f.type.startsWith('image/'));
		if (!file) {
			return;
		}

		this.readAndPreview(file);
		this.image.emit(file);
	}

	private getImageFromClipboard(data: DataTransfer | null): File | null {
		if (!data) return null;

		if (data.files && data.files.length) {
			const file = Array.from(data.files).find(f => f.type.startsWith('image/'));
			if (file) {
				return file;
			}
		}

		return null;
	}

	private readAndPreview(file: File) {
		const reader = new FileReader();
		reader.onload = () => {
			this.ngZone.run(() => this.previewUrl = reader.result);
		};
		reader.readAsDataURL(file);
	}

	clearImage() {
		this.previewUrl = null;
	}
}
