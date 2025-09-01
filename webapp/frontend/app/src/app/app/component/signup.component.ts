import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { User } from '../../model/user';
import { UserService } from '../../user.service';
import { environment } from 'src/environments/environment';

@Component({
	selector: 'minty-signup',
	imports: [CommonModule, FormsModule],
	templateUrl: 'signup.component.html',
	styleUrls: ['./signup.component.css']
})
export class SignupComponent {
	user: User;
	repeatPassword = '';
	passwordMismatch = true;
	messages: string[] = [];

	applicationName = environment.applicationName;

	constructor(private userService: UserService, private router: Router) {
		this.user = {
			id: '',
			name: '',
			password: '',
			defaults: new Map()
		};
	}

	signup(): boolean {
		this.messages = [];
		if (this.formValid()) {
			this.userService.signup(this.user)
				.subscribe({
					next: () => {
						this.userService.login(this.user.name, this.user.password)
							.subscribe({
								next: () => {
									this.router.navigateByUrl('/assistants');
									return true;
								},
								error: (error) => this.messages = error
							});
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
}
