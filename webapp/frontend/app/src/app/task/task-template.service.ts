import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, map } from 'rxjs/operators';
import { EMPTY, Observable } from 'rxjs';
import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';
import { TrackableSubject } from '../trackable-subject';
import { TaskDescription } from '../model/task-description';

@Injectable({
	providedIn: 'root'
})
export class TaskTemplateService {

	private resultListSubject: TrackableSubject<string[]> = new TrackableSubject<string[]>();
	resultList$: Observable<string[]> = this.resultListSubject.asObservable();

	private static readonly ListTemplates = 'api/task/template/list'; // List all templates that can be used to create tasks
	private static readonly GetTemplateConfiguration = 'api/task/template/config'; // Get description of the configuration of a task template
 
	private static readonly ListOutputTemplates = 'api/task/template/output/list'; // List all output tasks that can be used to format task output
	private static readonly GetOutputTemplatesConfiguration = 'api/task/template/output/config'; // Get description of the configuration of an output template
 
	constructor(private http: HttpClient, private alertService: AlertService) {
	}

	listTemplates(): Observable<TaskDescription[]> {
		return this.http.get<ApiResult>(TaskTemplateService.ListTemplates)
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					const returnValue = result.data as TaskDescription[];
					returnValue.forEach(value => {
						value.configuration = new Map(Object.entries(value.configuration));
					});

					return returnValue;
				})
			);
	}

	getTemplateConfiguration(taskId: number): Observable<Map<string, string>> {
		let params: HttpParams = new HttpParams();
		params = params.append('taskId', taskId);

		return this.http.get<ApiResult>(TaskTemplateService.GetTemplateConfiguration, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return new Map<string, string>(Object.entries(result.data as any));
				})
			);
	}

	getOutputTemplateConfiguration(taskId: number): Observable<Map<string, string>> {
		let params: HttpParams = new HttpParams();
		params = params.append('taskId', taskId);

		return this.http.get<ApiResult>(TaskTemplateService.GetOutputTemplatesConfiguration, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return new Map<string, string>(Object.entries(result.data as any));
				})
			);
	}

	listOutputTemplates(): Observable<TaskDescription[]> {
	return this.http.get<ApiResult>(TaskTemplateService.ListOutputTemplates)
		.pipe(
			catchError(error => {
				this.alertService.postFailure(JSON.stringify(error));
				return EMPTY;
			}),
			map((result: ApiResult) => {
				const returnValue = result.data as TaskDescription[];
				returnValue.forEach(value => {
					value.configuration = new Map(Object.entries(value.configuration));
				});

				return returnValue;
			})
		);
	}

}