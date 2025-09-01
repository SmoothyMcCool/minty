import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../alert.service';
import { User } from '../../model/user';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-user',
	imports: [CommonModule, FormsModule],
	templateUrl: 'view-user.component.html',
	styleUrls: ['./view-user.component.css']
})
export class ViewUserComponent implements OnInit {
	user: User;
	repeatPassword = '';
	passwordMismatch = true;
	messages: string[] = [];
	defaultValues: { key: string, value: string }[] = [];

	constructor(private userService: UserService, private router: Router, private alertService: AlertService) {
	}

	ngOnInit() {
		this.user = this.userService.getUser();
		this.userService.userDefaults().subscribe(userDefaults => {
			this.user.defaults = userDefaults;
			this.defaultValues = Array.from(this.user.defaults.entries())
				.map(([key, value]) => ({ key,value }));
		});
	}

	update(): boolean {
		this.messages = [];
		if (this.formValid()) {
			// Remove all blank or whitespace-only default values.
			this.user.defaults = new Map([...this.user.defaults.entries()]
				.filter(([key, value]) => !this.isBlank(value)));

			this.userService.update(this.user)
				.subscribe({
					next: () => {
						this.user = this.userService.getUser();
						this.alertService.postSuccess("Ok I updated you huzzah.")
						return true;
					},
					error: (error: string[]) => this.messages = error
				});
			return false;
		} else {
			this.messages = [ 'Hey! There\'s invalid crap!.' ];
			return false;
		}
	}

	passwordUpdated(): void {
		this.passwordMismatch = this.user.password !== this.repeatPassword || this.user.password.length < 8;
	}

	formValid(): boolean {
		return !(this.passwordMismatch || this.user.name.length === 0);
	}

	updateConfig(key: string , value: string) {
		this.user.defaults.set(key, value);
	}

	private isBlank(str: string) {
		return (!str || /^\s*$/.test(str));
	}
}
