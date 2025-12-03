import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
	selector: 'minty-view-help',
	imports: [CommonModule, RouterModule],
	templateUrl: 'view-help.component.html'
})
export class ViewHelpComponent {

	constructor(private router: Router) {
	}

	navigateTo(route: string) {
		this.router.navigateByUrl(route);
	}
}
