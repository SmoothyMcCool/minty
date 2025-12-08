import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { ApiResult } from './model/api-result';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from './alert.service';
import { MintyTool } from './model/minty-tool';

@Injectable({
	providedIn: 'root'
})

export class ToolService {

	private static readonly ListTools = 'api/tools';

	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	list(): Observable<MintyTool[]> {
		return this.http.get<ApiResult>(ToolService.ListTools)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as MintyTool[];
				})
			);
	}

}