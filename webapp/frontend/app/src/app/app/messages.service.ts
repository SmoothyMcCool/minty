import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ApiResult } from '../model/api-result';

@Injectable({
	providedIn: 'root'
})
export class MessagesService {

	private readonly url = '/api/messages';

	constructor(private http: HttpClient) {
	}

	getRandomMessage(): Observable<string> {
		return this.http.get<ApiResult>(`${this.url}/random`).pipe(
			map((result: ApiResult) => {
				return result.data as string;
			})
		);
	}

	getMessageOfTheDay(): Observable<string> {
		return this.http.get<ApiResult>(`${this.url}/motd`).pipe(
			map((result: ApiResult) => {
				return result.data as string;
			})
		);
	}
}