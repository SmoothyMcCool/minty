import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { Event, NavigationEnd, NavigationStart, Router, RouterEvent, RouterModule } from '@angular/router';
import { Alert, AlertService } from '../../alert.service';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { UserService } from '../../user.service';
import { Popover } from 'bootstrap';


@Component({
	selector: 'minty-app',
	imports: [CommonModule, RouterModule],
	encapsulation: ViewEncapsulation.None,
	templateUrl: 'app.component.html',
	styleUrls: ['./app.component.css']
})

export class AppComponent implements OnInit {
	alertList = new Map();
	syncInProgress = false;
	syncCounterDisplayed = false;
	sessionActive = false;
	connectionActive = true;

	constructor(private router: Router,
		private userService: UserService,
		private alertService: AlertService) {
		router.events.pipe(
			filter((e: Event | RouterEvent): e is RouterEvent => e instanceof RouterEvent)
		).subscribe((e: RouterEvent) => {
			if (e instanceof NavigationStart) {
				document.querySelectorAll('.popover').forEach(el => {
					const popover = Popover.getOrCreateInstance(el);
					popover.dispose();
				});
			}
		});
	}

	ngOnInit(): void {
		this.alertList.set('failure', []);
		this.alertList.set('info', []);
		this.alertList.set('success', []);
		this.alertService.alert.subscribe(item => this.handleAlert(item));

		this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: NavigationEnd) => {
			if (!this.userService.loggedIn() && event.url != '/signup') {
				this.logout();
			}
		});
	}


	handleAlert(alert: Alert): void {
		if (!alert) {
			return;
		}
		const alerts: string[] = this.alertList.get(alert.type);
		alerts.push(alert.message);
		if (alert.type === 'success') {
			$('#success-alert').fadeTo(500, 1).delay(5000).slideUp(500, () => {
				this.alertList.set('success', []);
			});
		} else if (alert.type === 'info') {
			$('#info-alert').fadeTo(500, 1).delay(10000).slideUp(500, () => {
				this.alertList.set('info', []);
			});
		} else if (alert.type === 'failure') {
			$('#failure-alert').fadeTo(500, 1);
		}
	}

	hideError(): void {
		$('#failure-alert').slideUp(() => {
			this.alertList.set('failure', []);
		});
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

	logout() {
		this.userService.logout();
	}
}
