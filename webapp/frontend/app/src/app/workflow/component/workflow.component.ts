import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AlertService } from 'src/app/alert.service';
import { Workflow } from 'src/app/model/workflow';
import { WorkflowService } from 'src/app/workflow/workflow.service';
import { TaskTemplateService } from 'src/app/task/task-template.service';
import { TaskEditorComponent } from 'src/app/task/component/task-editor.component';
import { TaskDescription } from 'src/app/model/task-description';

@Component({
    selector: 'minty-workflow',
    imports: [CommonModule, FormsModule, TaskEditorComponent],
    templateUrl: 'workflow.component.html',
    styleUrls: ['../../global.css', 'workflow.component.css']
})
export class WorkflowComponent implements OnInit {

    workflow: Workflow = {
        id: 0,
        ownerId: 0,
        name: '',
        description: '',
        shared: false,
        workflowSteps: [],
        outputStep: {
            name: '',
            configuration: new Map()
        }
    };
    taskTemplates: TaskDescription[] = [];
    outputTemplates: TaskDescription[] = [];

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private workflowService: WorkflowService,
        private taskTemplateService: TaskTemplateService,
        private alertService: AlertService) {
    }

    ngOnInit(): void {
        const workflowId = Number(this.route.snapshot.paramMap.get('id'));

        this.workflowService.getWorkflow(workflowId).subscribe((workflow: Workflow) => {
            this.workflow = workflow;

            this.taskTemplateService.listTemplates().subscribe(templates => {
                this.taskTemplates = templates;
            });

            this.taskTemplateService.listOutputTemplates().subscribe(output => {
                this.outputTemplates = output;
            });

        });
    }

    submit() {
        this.workflowService.execute(this.workflow).subscribe((result: string) => {
            this.alertService.postSuccess(result);
        });
        this.router.navigateByUrl('workflow');
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
