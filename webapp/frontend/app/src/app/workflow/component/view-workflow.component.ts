import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { RouterModule } from '@angular/router';

@Component({
	selector: 'minty-view-workflow',
	imports: [FormsModule, RouterModule],
	templateUrl: 'view-workflow.component.html',
	styleUrls: ['workflow.component.css']
})
export class ViewWorkflowComponent {
}
