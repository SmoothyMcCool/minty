import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { User } from './model/user';
import { catchError, EMPTY, finalize, map, Observable } from 'rxjs';
import { ApiResult } from './model/api-result';
import { Router } from '@angular/router';
import { AlertService } from './alert.service';

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

	private user: User = null;

	constructor(private alertService: AlertService, private http: HttpClient, private router: Router) { }

	loggedIn(): boolean {
		return this.user != null && sessionStorage.getItem('x-auth-token') != undefined;
	}

	getUser(): User {
		if (this.user === null) {
			this.router.navigateByUrl('/login');
		}
		return this.user;
	}

	login(account: string, password: string): Observable<User> {
		const headers = new HttpHeaders({
			authorization: 'Basic ' + btoa(account + ':' + password)
		});
		return this.http.get<ApiResult>(UserService.Login, { headers: headers, observe: 'response' })
			.pipe(
				map((result: HttpResponse<ApiResult>) => {
					sessionStorage.setItem('x-auth-token', result.headers.get('x-auth-token'));
					this.user = result.body.data as User;
					this.user.defaults = new Map(Object.entries(this.user.defaults));
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
			defaults: Object.fromEntries(user.defaults)
		};
		return this.http.post<ApiResult>(UserService.Update, body)
			.pipe(
				map((result: ApiResult) => {
					this.user = result.data as User;
					return this.user;
				})
			);
	}

	systemDefaults(): Observable<Map<string, string>> {
		return this.http.get<ApiResult>(UserService.GetSystemDefaults)
			.pipe(
				map((result: ApiResult) => {
					return new Map(Object.entries(result.data));
				})
			);
	}

	userDefaults(): Observable<Map<string, string>> {
		return this.http.get<ApiResult>(UserService.GetUserDefaults)
			.pipe(
				map((result: ApiResult) => {
					return new Map(Object.entries(result.data));
				})
			);
	}

	logout(): void {
		this.http.post<ApiResult>(UserService.Logout, {})
			.pipe(
				finalize(() => {
					this.user = null;
					sessionStorage.clear();
					this.router.navigateByUrl('/login');
				})
			).subscribe();
	}
}