import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { ActivatedRoute, Event, NavigationEnd, NavigationStart, Params, Router, RouterEvent, RouterModule } from '@angular/router';
import { Alert, AlertService, AlertType } from '../../alert.service';
import { combineLatest, filter } from 'rxjs/operators';

import { UserService } from '../../user.service';
import { Popover } from 'bootstrap';
import { User } from '../../model/user';
import { trigger, transition, style, animate } from '@angular/animations';
import { ProjectService } from '../../project/project.service';
import { Project } from '../../model/project/project';
import { FormsModule } from '@angular/forms';


@Component({
	selector: 'minty-app',
	imports: [FormsModule, RouterModule],
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

	activeProject: Project | undefined = undefined;
	projects: Project[] = [];

	currentUrl: string = '';

	newProjectVisible = false;
	newProjectName = '';

	private alertTimers = new Map<string, any>();

	constructor(private router: Router,
		private route: ActivatedRoute,
		private userService: UserService,
		private projectService: ProjectService,
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
			this.currentUrl = event.urlAfterRedirects;

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

		this.projectService.projectList$.subscribe(projects => {
			this.projects = projects;
		});

		this.projectService.activeProject$.subscribe(activeProject => {
			this.activeProject = activeProject;
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

	navigateTo(url: string, queryParams?: Params): void {
		this.router.navigate([url], { queryParams, queryParamsHandling: 'merge' });
	}

	logout() {
		this.userService.logout();
	}

	projectChanged(projectId: string) {
		const activeProject = this.projects.find(item => item.id === projectId);
		if (activeProject) {
			this.projectService.setActiveProject(activeProject);
			this.router.navigate([], {
				queryParams: {
					projectId: activeProject.id
				},
				queryParamsHandling: 'merge'
			});
		}
	}

	isRouteActive(route: string): boolean {
		return this.currentUrl.includes(route);
	}

	createNewProject() {
		this.newProjectVisible = false;
		this.projectService.createProject(this.newProjectName).subscribe(() => {
			this.alertService.postSuccess('Project created!');
		})
	}

	cancelNewProject() {
		this.newProjectVisible = false;
		this.newProjectName = '';
	}

}
