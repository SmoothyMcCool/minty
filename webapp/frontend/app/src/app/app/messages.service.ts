import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ApiResult } from '../model/api-result';

@Injectable({
	providedIn: 'root'
})
export class MessagesService {

	private static readonly DailyMesssage = 'api/messages/motd';
	private static readonly RandomMesssage = 'api/messages/random';

	constructor(private http: HttpClient) {
	}

	getRandomMessage(): Observable<string> {
		return this.http.get<ApiResult>(MessagesService.RandomMesssage).pipe(
			map((result: ApiResult) => {
				return result.data as string;
			})
		);
	}

	getMessageOfTheDay(): Observable<string> {
		return this.http.get<ApiResult>(MessagesService.DailyMesssage).pipe(
			map((result: ApiResult) => {
				return result.data as string;
			})
		);
	}
}