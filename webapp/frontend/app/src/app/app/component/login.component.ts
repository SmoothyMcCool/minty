import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { UserService } from '../../user.service';
import { environment } from '../../../environments/environment';
import { DidYouKnowComponent } from './did-you-know.component';
import { MessagesService } from '../messages.service';

@Component({
	selector: 'minty-login',
	imports: [FormsModule, DidYouKnowComponent],
	templateUrl: 'login.component.html',
	styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

	loginFailed = false;
	credentials = {
		account: '',
		password: ''
	};

	applicationName = environment.applicationName;

	motd = '';
	message = '';

	constructor(private userService: UserService,
		private messagesService: MessagesService,
		private router: Router) {
	}

	ngOnInit() {
		this.messagesService.getMessageOfTheDay().subscribe(motd => {
			this.motd = motd;
		});
		this.messagesService.getRandomMessage().subscribe(message => {
			this.message = message;
		});
	}

	login(): boolean {
		sessionStorage.clear();
		this.loginFailed = false;
		this.userService.login(this.credentials.account, this.credentials.password)
			.subscribe({
				next: () => {
					this.router.navigate(['/projects'], { queryParamsHandling: 'merge' });
				},
				error: () => {
					this.loginFailed = true;
				}
			});
		return false;
	}

	signup(): void {
		this.router.navigateByUrl('/signup');
	}

}
