import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { AlertService } from "../alert.service";
import { catchError, EMPTY, map, Observable, of, switchMap, timer } from "rxjs";
import { Diagram } from "../model/diagram/diagram";
import { ApiResult } from "../model/api-result";

@Injectable({
	providedIn: 'root'
})
export class DiagramService {

	private static readonly RequestDiagram = 'api/diagram/ask';
	private static readonly RetrieveDiagram = 'api/diagram/get';

	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	ask(request: string): Observable<string> {
		let params: HttpParams = new HttpParams();
		params = params.append('request', request);

		return this.http.get<ApiResult>(DiagramService.RequestDiagram, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as string;
				})
			);
	}

	get(requestId: string): Observable<string> {
		let params: HttpParams = new HttpParams();
		params = params.append('requestId', requestId);

		return this.http.get<ApiResult>(DiagramService.RetrieveDiagram, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				switchMap((result: ApiResult) => {
					if (result.data === '~~Not~Running~~') {
						return EMPTY;
					}
					if (result.data === '~~LLM~Executing~~') {
						return timer(5000).pipe(
							switchMap(() => this.get(requestId))
						);
					}
					return of(result.data as string);
				})
			);
	}
}