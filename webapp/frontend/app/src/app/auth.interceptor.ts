import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { EMPTY, Observable } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class AuthorizedInterceptor implements HttpInterceptor {

    private loginUrl = 'login';

    constructor(private router: Router) { }

    intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        return next.handle(request)
            .pipe(
                catchError(event => {
                    if (event instanceof HttpErrorResponse && event.status === 401 || event.status === 403) {
                        if (event.url.endsWith('login')) {
                            throw { message: 'Login Failed!' };
                        }
                        this.router.navigate([this.loginUrl]);
                        return EMPTY;
                    }
                    throw event;
                })
            );
    }

}
