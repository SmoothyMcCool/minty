import { Injectable } from '@angular/core';
import { catchError, EMPTY, map, Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { AlertService } from '../alert.service';
import { ApiResult } from '../model/api-result';
import { SkillMetadata, Skill } from '../model/skills/skill';

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