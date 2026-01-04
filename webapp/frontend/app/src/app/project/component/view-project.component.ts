import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
	selector: 'minty-view-project',
	imports: [CommonModule, FormsModule, RouterModule],
	templateUrl: 'view-project.component.html'
})
export class ViewProjectComponent {
}
