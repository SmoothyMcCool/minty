import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'minty-confirmation-dialog',
  imports: [CommonModule],
  templateUrl: './confirmation-dialog.component.html'
})
export class ConfirmationDialogComponent {
	@Input() title: string = '';
	@Input() message: string = '';
	private _visible = false;
	@Input() set visible(value: boolean) {
		this._visible = value;
		document.body.classList.toggle('modal-open', value);
	}
	get visible(): boolean {
	return this._visible;
	}

	@Output() confirm = new EventEmitter<boolean>();
	@Output() cancel = new EventEmitter<boolean>();

	onConfirm() {
		this.visible = false;
		this.confirm.emit();
	}

	onCancel() {
		this.visible = false;
		this.cancel.emit();
	}
}
