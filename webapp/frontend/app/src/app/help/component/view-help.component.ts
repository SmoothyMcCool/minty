import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';


@Component({
	selector: 'minty-view-help',
	imports: [RouterModule],
	templateUrl: 'view-help.component.html'
})
export class ViewHelpComponent {

	constructor(private router: Router) {
	}

	navigateTo(route: string) {
		this.router.navigate([route], { queryParamsHandling: 'merge' });
	}
}
