import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
    selector: 'minty-view-task',
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: 'view-task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class ViewTaskComponent {

}
