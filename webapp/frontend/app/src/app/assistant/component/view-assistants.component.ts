import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';
import { User, DisplayMode } from 'src/app/model/user';
import { UserService } from 'src/app/user.service';

@Component({
	selector: 'minty-view-assistants',
	imports: [CommonModule, RouterModule],
	templateUrl: 'view-assistants.component.html'
})
export class ViewAssistantsComponent {

	user: User;
	DisplayMode = DisplayMode;

	constructor(private userService: UserService) {
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
	}
}
