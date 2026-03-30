import { Injectable } from '@angular/core';
import { catchError, EMPTY, map, Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';
import { SkillMetadata, Skill } from '../model/skills/skill';
import { UserSelection } from '../app/component/user-select-dialog.component';

@Injectable({
	providedIn: 'root'
})
export class SkillService {

	// -------------------------
	// ROUTES
	// -------------------------

	private static readonly ListSkills = 'api/skills/list';
	private static readonly GetSkill = 'api/skills';
	private static readonly UploadSkill = 'api/skills/upload';
	private static readonly DeleteSkill = 'api/skills/delete';
	private static readonly ShareSkill = 'api/skills/share';
	private static readonly ListSharedUsers = 'api/skills/getsharing';

	constructor(private http: HttpClient,
		private alertService: AlertService) { }

	// -------------------------
	// SKILLS
	// -------------------------

	listSkills(): Observable<SkillMetadata[]> {
		return this.http.get<ApiResult>(SkillService.ListSkills).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as SkillMetadata[])
		);
	}

	getSkill(name: string): Observable<Skill> {
		const params = new HttpParams()
			.set('name', name);

		return this.http.get<ApiResult>(SkillService.GetSkill, { params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as Skill)
		);
	}

	uploadSkill(file: File): Observable<string> {
		const formData = new FormData();
		formData.append('file', file, file.name);

		return this.http.post<ApiResult>(SkillService.UploadSkill, formData).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as string)
		);
	}

	deleteSkill(name: string): Observable<string> {
		const params = new HttpParams()
			.set('name', name);

		return this.http.delete<ApiResult>(SkillService.DeleteSkill, { params }).pipe(
			this.handleError(),
			map((result: ApiResult) => result.data as string)
		);
	}

	shareSkill(name: string, userSelection: UserSelection): Observable<string> {
		const body = {
			resource: name,
			userSelection: userSelection
		}
		return this.http.post<ApiResult>(SkillService.ShareSkill, body)
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

	getSharingList(name: string): Observable<UserSelection> {
		let params: HttpParams = new HttpParams();
		params = params.append('name', name);

		return this.http.get<ApiResult>(SkillService.ListSharedUsers, { params: params })
			.pipe(
				catchError(error => {
					this.alertService.postFailure(JSON.stringify(error));
					return EMPTY;
				}),
				map((result: ApiResult) => {
					return result.data as UserSelection;
				})
			);
	}

	// -------------------------
	// ERROR HANDLER
	// -------------------------

	private handleError() {
		return catchError(error => {
			this.alertService.postFailure(JSON.stringify(error));
			return EMPTY;
		});
	}
}