import { HttpClient, HttpHeaders, HttpResponse } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AlertService } from "./alert.service";
import { User } from "./model/user";
import { finalize, map, Observable } from "rxjs";
import { ApiResult } from "./model/api-result";
import { Router } from "@angular/router";

@Injectable({
    providedIn: 'root'
})

export class UserService {

    private static readonly Login = 'api/login';
    private static readonly Logout = 'api/logout';
    private static readonly Signup = 'api/user/new';
    private static readonly Update = 'api/user/update';

    private user: User = null;

    constructor(private http: HttpClient, private router: Router) { }

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
                    return this.user;
                })
            );
    }

    public signup(user: User): Observable<User> {
        return this.http.post<ApiResult>(UserService.Signup, user)
            .pipe(
                map((result: ApiResult) => {
                    this.user = result.data as User;
                    return this.user;
                })
            );
    }

    public update(user: User): Observable<User> {
        return this.http.post<ApiResult>(UserService.Update, user)
            .pipe(
                map((result: ApiResult) => {
                    this.user = result.data as User;
                    return this.user;
                })
            );
    }

    public logout(): void {
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