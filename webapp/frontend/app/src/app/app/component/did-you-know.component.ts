
import { Component, Input } from '@angular/core';

@Component({
	selector: 'minty-did-you-know',
	standalone: true,
	templateUrl: './did-you-know.component.html',
	styleUrls: ['./did-you-know.component.css']
})
export class DidYouKnowComponent {
	@Input() message = '';
}