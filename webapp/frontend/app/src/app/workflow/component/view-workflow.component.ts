import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DisplayMode, User } from 'src/app/model/user';
import { UserService } from 'src/app/user.service';

@Component({
	selector: 'minty-view-workflow',
	imports: [CommonModule, FormsModule, RouterModule],
	templateUrl: 'view-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class ViewWorkflowComponent {

	user: User;
	DisplayMode = DisplayMode;

	public constructor(private userService: UserService) {
		this.user = this.userService.getUser();
	}
}
