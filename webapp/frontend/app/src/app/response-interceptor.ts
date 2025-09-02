import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

@Injectable()
export class ResponseInterceptor implements HttpInterceptor {
	intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
		return next.handle(req).pipe(
			map(event => {
				if (event instanceof HttpResponse) {
					if (event.body && !event.body.ok) {
						const messages = [];
						for (let i = 0; i < event.body.messages.length; i++) {
							messages.push(event.body.messages[i]);
						}
						throw event.body;
					}
				}
				return event;
			})
		);
	}

}