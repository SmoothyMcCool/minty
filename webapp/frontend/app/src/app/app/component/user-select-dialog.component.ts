import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { UserService } from '../../user.service';

export interface UserSelection {
	allUsers: boolean;
	selectedUsers: string[];
}

@Component({
	selector: 'minty-user-select-dialog',
	imports: [CommonModule],
	templateUrl: './user-select-dialog.component.html'
})
export class UserSelectDialogComponent implements OnChanges {
	@Input() visible: boolean = false;
	@Input() initialSelection: UserSelection = { allUsers: false, selectedUsers: [] };

	@Output() confirm = new EventEmitter<UserSelection>();
	@Output() cancel = new EventEmitter<void>();

	users: string[] = [];
	selectedUsers: Set<string> = new Set();
	allSelected: boolean = false;

	constructor(private userService: UserService) { }

	ngOnChanges(): void {
		if (this.visible) {
			this.allSelected = this.initialSelection?.allUsers;
			if (Array.isArray(this.initialSelection?.selectedUsers)) {
				this.selectedUsers = new Set(this.initialSelection.selectedUsers);
			}
			this.userService.listUsers().subscribe(users => this.users = users);
		}
	}

	isSelected(user: string): boolean {
		return this.selectedUsers.has(user);
	}

	onToggleUser(user: string): void {
		if (this.selectedUsers.has(user)) {
			this.selectedUsers.delete(user);
		} else {
			this.selectedUsers.add(user);
		}
	}

	onSelectAll(): void {
		this.allSelected = !this.allSelected;
		if (this.allSelected) {
			this.selectedUsers.clear();
		}
	}

	onConfirm(): void {
		this.visible = false;
		this.confirm.emit({
			allUsers: this.allSelected,
			selectedUsers: this.allSelected ? [] : Array.from(this.selectedUsers)
		});
		this.initialSelection = { allUsers: false, selectedUsers: [] };
	}

	onCancel(): void {
		this.visible = false;
		this.cancel.emit();
		this.initialSelection = { allUsers: false, selectedUsers: [] };
	}
}