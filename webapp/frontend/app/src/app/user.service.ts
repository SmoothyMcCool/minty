import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { User } from './model/user';
import { catchError, EMPTY, finalize, map, Observable, of, startWith, Subject, switchMap, takeUntil, timer } from 'rxjs';
import { ApiResult } from './model/api-result';
import { Router } from '@angular/router';
import { AlertService } from './alert.service';
import { AttributeMap } from './model/workflow/task-specification';

@Injectable({
	providedIn: 'root'
})

export class UserService {

	private static readonly Login = 'api/login';
	private static readonly Logout = 'api/logout';
	private static readonly Signup = 'api/user/new';
	private static readonly Update = 'api/user/update';
	private static readonly GetSystemDefaults = 'api/user/defaults/system';
	private static readonly GetUserDefaults = 'api/user/defaults/user';
	private static readonly GetUser = 'api/user';

	private user: User = null;
	private timeoutTimer$ = new Subject<'start' | 'stop'>();

	constructor(private alertService: AlertService, private http: HttpClient, private router: Router) {
		this.startSessionTimeout();
	}

	loggedIn(): boolean {
		return sessionStorage.getItem('x-auth-token') != undefined;
	}

	getUser(): Observable<User> {
		if (this.user === null) {
			return this.http.get<ApiResult>(UserService.GetUser)
				.pipe(
					map((result: ApiResult) => {
						this.user = result.data as User;
						this.user.defaults = { ...this.user.defaults };
						return this.user;
					})
				);
		}
		return of(this.user);
	}

	login(account: string, password: string): Observable<User> {
		const headers = new HttpHeaders({
			authorization: 'Basic ' + btoa(account + ':' + password)
		});
		return this.http.get<ApiResult>(UserService.Login, { headers: headers, observe: 'response' })
			.pipe(
				map((result: HttpResponse<ApiResult>) => {
					this.timeoutTimer$.next('start');
					sessionStorage.setItem('x-auth-token', result.headers.get('x-auth-token'));
					this.user = result.body.data as User;
					this.user.defaults = { ...this.user.defaults };
					return this.user;
				})
			);
	}

	signup(user: User): Observable<User> {
		return this.http.post<ApiResult>(UserService.Signup, user)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					this.user = result.data as User;
					return this.user;
				})
			);
	}

	update(user: User): Observable<User> {
		const body = {
			id: user.id,
			name: user.name,
			password: user.password,
			defaults: { ...user.defaults },
			settings: { ...user.settings }
		};
		return this.http.post<ApiResult>(UserService.Update, body)
			.pipe(
				map((result: ApiResult) => {
					this.user = result.data as User;
					return this.user;
				})
			);
	}

	systemDefaults(): Observable<AttributeMap> {
		return this.http.get<ApiResult>(UserService.GetSystemDefaults)
			.pipe(
				map((result: ApiResult) => {
					return result.data as AttributeMap;
				})
			);
	}

	userDefaults(): Observable<AttributeMap> {
		return this.http.get<ApiResult>(UserService.GetUserDefaults)
			.pipe(
				map((result: ApiResult) => {
					return result.data as AttributeMap;
				})
			);
	}

	logout(): void {
		this.http.post<ApiResult>(UserService.Logout, {})
			.pipe(
				finalize(() => {
					this.timeoutTimer$.next('stop');
					this.user = null;
					sessionStorage.clear();
					this.router.navigateByUrl('/login');
				})
			).subscribe();
	}

	startSessionTimeout() {
		this.timeoutTimer$.pipe(
			switchMap(state =>
				state === 'start' ? timer(240 * 60 * 1000) : EMPTY // 240m timeout, to match backend
			)
		).subscribe(() => {
			this.logout();
		});
	}

	resetSessionTimeout() {
		this.timeoutTimer$.next('start'); // reset timer
	}
}