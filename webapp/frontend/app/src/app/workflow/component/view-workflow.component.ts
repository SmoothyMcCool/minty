import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'ai-view-workflow',
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: 'view-workflow.component.html',
    styleUrls: ['../../global.css', 'workflow.component.css']
})
export class ViewWorkflowComponent {

}
