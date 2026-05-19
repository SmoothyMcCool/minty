import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { Event, NavigationEnd, NavigationStart, Router, RouterEvent, RouterModule } from '@angular/router';
import { Alert, AlertService, AlertType } from '../../alert.service';
import { filter } from 'rxjs/operators';

import { UserService } from '../../user.service';
import { Popover } from 'bootstrap';
import { User } from '../../model/user';
import { trigger, transition, style, animate } from '@angular/animations';


@Component({
	selector: 'minty-app',
	imports: [RouterModule],
	encapsulation: ViewEncapsulation.None,
	templateUrl: 'app.component.html',
	styleUrls: ['./app.component.css'],
	animations: [
		trigger('alertAnim', [

			// ENTER (fade in)
			transition(':enter', [
				style({ opacity: 0, transform: 'translateY(-8px)' }),
				animate('200ms ease-out',
					style({ opacity: 1, transform: 'translateY(0)' })
				)
			]),

			// EXIT (fade out)
			transition(':leave', [
				animate('200ms ease-in',
					style({ opacity: 0, transform: 'translateY(-8px)' })
				)
			])
		])
	]
})

export class AppComponent implements OnInit {
	alerts = {
		success: [] as string[],
		info: [] as string[],
		failure: [] as string[]
	};

	syncInProgress = false;
	syncCounterDisplayed = false;
	sessionActive = false;
	connectionActive = true;
	user!: User;

	private alertTimers = new Map<string, any>();

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
		const mapThemeLabelToValue = (label?: string): 'light' | 'dark' => {
			if (!label) return 'light';
			return label.toLowerCase().includes('dark') ? 'dark' : 'light';
		};

		this.alerts.failure = [];
		this.alerts.info = [];
		this.alerts.success = [];
		this.alertService.alert.subscribe(item => this.handleAlert(item));

		this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe((event: NavigationEnd) => {
			if (!this.userService.loggedIn() && event.url != '/signup') {
				this.logout();
			}
			if (this.userService.loggedIn()) {
				this.userService.getUser().subscribe(user => {
					this.user = user;
					const theme = document.documentElement.setAttribute('data-bs-theme', mapThemeLabelToValue(this.user.settings['Theme'] ?? 'Light Mode'));
				});
			}
		});
	}

	handleAlert(alert: Alert): void {
		if (!alert || !alert.type || !alert.message) {
			return;
		}

		const type = alert.type.toLowerCase();

		if (type !== 'success' && type !== 'info' && type !== 'failure') {
			console.warn('Unknown alert type:', alert.type);
			return;
		}

		const typed = type as AlertType;

		this.alerts[typed] = [...this.alerts[typed], alert.message];

		// clear previous timer
		if (this.alertTimers.has(typed)) {
			clearTimeout(this.alertTimers.get(typed));
		}

		if (typed === 'success') {
			this.alertTimers.set(
				typed,
				setTimeout(() => {
					this.alerts.success = [];
				}, 5500)
			);
		}

		else if (typed === 'info') {
			this.alertTimers.set(
				typed,
				setTimeout(() => {
					this.alerts.info = [];
				}, 10500)
			);
		}

		// failure = persistent
	}

	hideError(): void {
		this.alerts.failure = [];
	}

	navigateTo(url: string): void {
		this.router.navigateByUrl(url);
	}

	logout() {
		this.userService.logout();
	}
}
