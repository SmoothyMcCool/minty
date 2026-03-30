import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Skill, SkillFile, SkillMetadata } from 'src/app/model/skills/skill';
import { SkillService } from '../skill.service';
import { MarkdownModule } from 'ngx-markdown';
import { MermaidClipboardDirective } from 'src/app/assistant/component/mermaid-clipboard.directive';
import { AlertService } from 'src/app/alert.service';
import { ConfirmationDialogComponent } from 'src/app/app/component/confirmation-dialog.component';
import { UserSelectDialogComponent, UserSelection } from 'src/app/app/component/user-select-dialog.component';

@Component({
	selector: 'minty-skill-viewer',
	imports: [CommonModule, MarkdownModule, MermaidClipboardDirective, ConfirmationDialogComponent, UserSelectDialogComponent],
	templateUrl: 'skill-viewer.component.html',
})
export class SkillViewerComponent {

	skills: SkillMetadata[] = [];
	selectedSkill: Skill | null = null;
	selectedFile: SkillFile | null = null;

	skillFileDialogVisible = false;
	skillFile: File;

	deleteSkillDialogVisible = false;
	skillToDelete: string;

	userSelectDialogVisible = false;
	skillToShare: string;
	sharingSelection: UserSelection;

	constructor(private skillService: SkillService, private alertService: AlertService) { }

	ngOnInit(): void {
		this.refreshSkillList();
	}

	onSelectSkill(metadata: SkillMetadata): void {
		this.skillService.getSkill(metadata.name).subscribe(skill => {
			this.selectedSkill = skill;
			this.selectedFile = skill.files?.length ? skill.files[0] : null;
		});
	}

	onSelectFile(file: SkillFile): void {
		this.selectedFile = file;
	}

	fileSelected(event: Event) {
		const newFiles = (event.target as HTMLInputElement).files;
		if (newFiles && newFiles.length > 0) {
			this.skillFile = newFiles[0];
		}
	}

	addSkill() {
		this.skillFileDialogVisible = false;
		this.skillService.uploadSkill(this.skillFile).subscribe(response => {
			this.alertService.postSuccess(response);
			this.refreshSkillList();
			this.skillFile = null;
		});
	}

	deleteSkill(name: string) {
		this.deleteSkillDialogVisible = true;
		this.skillToDelete = name;
	}

	confirmDeleteSkill() {
		this.deleteSkillDialogVisible = false;
		this.skillService.deleteSkill(this.skillToDelete).subscribe(response => {
			this.alertService.postSuccess(response);
			this.refreshSkillList();
			this.skillToDelete = null;
		});
	}

	shareSkill(name: string): void {
		this.userSelectDialogVisible = true;
		this.skillToShare = name;
		this.skillService.getSharingList(name).subscribe(selection => {
			this.sharingSelection = selection;
		});
	}

	onUsersConfirmed(selection: UserSelection): void {
		this.userSelectDialogVisible = false;
		this.skillService.shareSkill(this.skillToShare, selection).subscribe(response => {
			this.alertService.postSuccess(response);
			this.refreshSkillList();
			this.skillToShare = null;
		});
	}


	extractFrontmatter(content: string): string {
		const match = content.match(/^---\n([\s\S]*?)\n---/);
		return match ? match[1].trim() : '';
	}

	renderContent(content: string): string {
		return content.replace(/^---[\s\S]*?---\n?/, '').trim();
	}

	private refreshSkillList() {
		this.skillService.listSkills().subscribe(skills => {
			this.selectedSkill = null;
			this.skills = skills;
		});
	}
}
