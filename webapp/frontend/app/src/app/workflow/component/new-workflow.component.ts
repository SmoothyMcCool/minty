import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { WorkflowTask } from "../../model/workflow-task";
import { WorkflowService } from "../workflow.service";
import { Router } from "@angular/router";
import { WorkflowConfigComponent } from "./workflow-config.component";
import { AlertService } from "src/app/alert.service";

@Component({
    selector: 'ai-new-workflow-task',
    imports: [CommonModule, FormsModule, WorkflowConfigComponent],
    templateUrl: 'new-workflow.component.html',
    styleUrls: ['../../global.css', 'workflow.component.css']
})
export class NewWorkflowTaskComponent implements OnInit {

    workflows: Map<string, Map<string, string>> = new Map();

    task: WorkflowTask = {
        id: 0,
        name: "",
        workflow: "",
        description: "",
        defaultConfig: new Map<string, string>()
    };
    isFileTriggered: boolean = false;
    triggerDirectory: string = '';

    configParams = new Map<string, string>();

    constructor(private alertService: AlertService,
        private router: Router,
        private workflowService: WorkflowService) {
    }

    ngOnInit() {
        this.workflowService.listWorkflows().subscribe((workflows: any) => {
            this.workflows = workflows as Map<string, Map<string, string>>;
        });
    }

    createWorkflowTask() {
        if (this.isFileTriggered) {
            this.workflowService.newTriggeredTask(this.task, this.triggerDirectory).subscribe(() => {
                this.alertService.postSuccess('Workflow Created!');
                this.router.navigateByUrl('workflow');
            });
        }
        else {
            this.workflowService.newTask(this.task).subscribe(() => {
                this.alertService.postSuccess('Workflow Created!');
                this.router.navigateByUrl('workflow');
            });
        }
    }

    workflowChanged($event) {
        this.configParams = new Map(this.workflows.get($event));

        this.task.defaultConfig = new Map<string, string>();

        if (this.configParams !== undefined) {
            this.task.defaultConfig = new Map(this.configParams);
        }
        else {
            this.task.defaultConfig = new Map<string, string>();
        }
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
