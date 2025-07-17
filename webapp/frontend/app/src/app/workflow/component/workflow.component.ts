import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { WorkflowService } from '../workflow.service';
import { ActivatedRoute, Router } from '@angular/router';
import { WorkflowTask } from '../../model/workflow-task';
import { WorkflowConfigComponent } from './workflow-config.component';
import { AlertService } from 'src/app/alert.service';

@Component({
    selector: 'ai-workflow',
    imports: [CommonModule, FormsModule, WorkflowConfigComponent],
    templateUrl: 'workflow.component.html',
    styleUrls: ['../../global.css', 'workflow.component.css']
})
export class WorkflowComponent implements OnInit {

    task: WorkflowTask = {
        id: 0,
        workflow: '',
        name: '',
        description: '',
        defaultConfig: new Map<string, string>()
    };
    taskConfig: Map<string, string> = new Map();
    taskConfigParams: Map<string, string> = new Map();

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private workflowService: WorkflowService,
        private alertService: AlertService) {
    }

    ngOnInit(): void {
        const taskId = Number(this.route.snapshot.paramMap.get('id'));

        this.workflowService.getTask(taskId).subscribe((task: WorkflowTask) => {
            this.task = task;
            this.taskConfig = this.task.defaultConfig;

            this.workflowService.getTaskConfig(taskId).subscribe((taskConfigParams: Map<string, string>) => {
                this.taskConfigParams = taskConfigParams;
            })
        });       
    }

    submit() {
        this.workflowService.execute(this.task.workflow, this.taskConfig).subscribe((result: string) => {
            this.alertService.postSuccess(result);
        });
        this.router.navigateByUrl('workflow');
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
