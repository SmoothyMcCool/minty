import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { RouterModule, ActivatedRoute, Router } from "@angular/router";
import { ConfirmationDialogComponent } from "src/app/app/component/confirmation-dialog.component";
import { WorkflowResult } from "src/app/model/workflow/workflow-result";
import { ResultService } from "../result.service";


@Component({
	selector: 'minty-workflow-result',
	imports: [CommonModule, FormsModule, RouterModule, ConfirmationDialogComponent],
	templateUrl: 'display-workflow-result.component.html',
	styleUrls: ['workflow.component.css']
})
export class DisplayWorkflowResultComponent implements OnInit {

	result: WorkflowResult = null;
	responseType: string;

	constructor(private route: ActivatedRoute,
		private router: Router,
		private resultService: ResultService) {
	}

	ngOnInit() {
		this.result = null;
		this.route.params.subscribe(params => {
			this.resultService.getWorkflowResult(params['id']).subscribe(result => {
				if (this.endsWithIgnoreCase(result.outputFormat, 'text/json')) {
					this.responseType = 'JSON';
				} else if (this.endsWithIgnoreCase(result.outputFormat, 'text/html')) {
					this.responseType = 'HTML';
				} else {
					this.responseType = 'TEXT';
				}
				this.result = result;
			})
		});
	}

	private endsWithIgnoreCase(str: string, ending: string) {
		return str.toLowerCase().endsWith(ending.toLowerCase());
	}

}
