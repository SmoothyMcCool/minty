import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AlertService } from './alert.service';

@Injectable()
export class XhrInterceptor implements HttpInterceptor {

	constructor(private alertService: AlertService) { }

	intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
		let headers = req.headers;
		headers = req.headers.set('X-Requested-With', 'XMLHttpRequest');

		const token = sessionStorage.getItem('x-auth-token');
		if (token !== null) {
			headers = headers.set('x-auth-token', sessionStorage.getItem('x-auth-token'));
		}

		const xhr = req.clone({
			headers: headers
		});

		// Kinda kludgy thing to work around ExpressionChangedAfterItHasBeenCheckedError in ngIfs based on this value.
		setTimeout(() => { this.alertService.addOutstandingRequest(); }, 0);

		return next.handle(xhr);
	}
}
