import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Router } from "@angular/router";
import { AlertService } from "src/app/alert.service";
import { Task } from '../../model/task';
import { TaskTemplateService } from "src/app/task/task-template.service";
import { Workflow } from "src/app/model/workflow";
import { WorkflowService } from "../workflow.service";
import { TaskEditorComponent } from "src/app/task/component/task-editor.component";

@Component({
    selector: 'minty-new-workflow',
    imports: [CommonModule, FormsModule, TaskEditorComponent],
    templateUrl: 'new-workflow.component.html',
    styleUrls: ['../../global.css', 'workflow.component.css']
})
export class NewWorkflowComponent implements OnInit {

    taskTemplates: Map<string, Map<string, string>> = new Map();
    outputTaskTemplates: Map<string, Map<string, string>> = new Map();

    workflow: Workflow = {
        name: '',
        description: '',
        id: 0,
        shared: false,
        workflowSteps: [],
        outputStep: {
            name: '',
            configuration: new Map()
        }
    };

    isFileTriggered: boolean = false;
    triggerDirectory: string = '';

    configParams = new Map<string, string>();
    outputTaskConfigParams = new Map<string, string>();

    constructor(private alertService: AlertService,
        private router: Router,
        private workflowService: WorkflowService,
        private taskTemplateService: TaskTemplateService) {
    }

    ngOnInit() {
        this.taskTemplateService.listTemplates().subscribe((taskTemplates: Map<string, Map<string, string>>) => {
            this.taskTemplates = taskTemplates;
        });
        this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: Map<string, Map<string, string>>) => {
            this.outputTaskTemplates = outputTaskTemplates;
        });
    }

    createWorkflow() {
        this.workflowService.newWorkflow(this.workflow).subscribe(() => {
            this.alertService.postSuccess('Workflow Created!');
            this.router.navigateByUrl('workflow');
        });
    }

    addStep() {
        this.workflow.workflowSteps.push({
            name: '',
            configuration: new Map()
        })
    }

    deleteStep(index: number) {
        this.workflow.workflowSteps.splice(index, 1);
    }

    cancel() {
        this.router.navigateByUrl('workflow');
    }
}
