import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../alert.service';
import { User } from '../../model/user';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-user',
	imports: [CommonModule, FormsModule],
	templateUrl: 'view-user.component.html'
})
export class ViewUserComponent implements OnInit {
	user: User;
	repeatPassword = '';
	passwordMismatch = true;
	updatePassword = false;
	messages: string[] = [];
	defaultValues: { key: string, value: string }[] = [];
	messageOrder = 'Newest at Top';
	buttonAlignment = 'Left';
	theme = 'Light Mode';

	constructor(private userService: UserService, private alertService: AlertService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;
			this.messageOrder = this.user.settings['Message Order'] ?? 'Newest at Top';
			this.buttonAlignment = this.user.settings['Button Alignment'] ?? 'Left';
			this.theme = this.user.settings['Theme'] ?? 'Light Mode';
			this.userService.userDefaults().subscribe(userDefaults => {
				this.user.defaults = userDefaults;
				this.defaultValues = Object.entries(this.user.defaults).map(([key, value]) => ({ key,value }));
			});
		});
	}

	sortConversationsBy(sortOrder: string) {
		this.messageOrder = sortOrder;
		this.user.settings['Message Order'] = sortOrder;
	}

	useAlignment(alignment: string) {
		this.buttonAlignment = alignment;
		this.user.settings['Button Alignment'] = alignment;
	}

	useTheme(theme: string) {
		this.theme = theme;
		this.user.settings['Theme'] = theme;
	}

	update(): boolean {
		this.messages = [];
		if (this.formValid()) {
			// Remove all blank or whitespace-only default values.
			this.user.defaults = Object.fromEntries(
				Object.entries(this.user.defaults).filter(([, value]) => !this.isBlank(value))
			);

			this.userService.update(this.user)
				.subscribe({
					next: () => {
						this.userService.getUser().subscribe(user => {
							this.user = user;
							this.alertService.postSuccess('Ok I updated you huzzah.');
						});
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
		return !((this.updatePassword && this.passwordMismatch) || this.user.name.length === 0);
	}

	updateConfig(key: string , value: string) {
		this.user.defaults[key] = value;
	}

	private isBlank(str: string) {
		return (!str || /^\s*$/.test(str));
	}

}
