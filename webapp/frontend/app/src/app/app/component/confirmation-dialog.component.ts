import { CommonModule } from "@angular/common";
import { Component, EventEmitter, Input, Output } from "@angular/core";

@Component({
  selector: 'minty-confirmation-dialog',
  imports: [CommonModule],
  templateUrl: './confirmation-dialog.component.html'
})
export class ConfirmationDialogComponent {
    @Input() title: string = '';
    @Input() message: string = '';
    @Input() visible: boolean = false;

    @Output() confirm = new EventEmitter<boolean>();
    @Output() cancel = new EventEmitter<boolean>();

    onConfirm() {
        this.confirm.emit();
        this.visible = false;
    }

    onCancel() {
        this.cancel.emit();
        this.visible = false;
    }
}
