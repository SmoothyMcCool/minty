import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SkillViewerComponent } from './skill-viewer.component';

@Component({
	selector: 'minty-view-skills',
	imports: [CommonModule, FormsModule, RouterModule, SkillViewerComponent],
	templateUrl: 'view-skills.component.html'
})
export class ViewSkillsComponent {
}
