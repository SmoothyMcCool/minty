import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { TaskService } from "../task.service";
import { Router } from "@angular/router";
import { AlertService } from "src/app/alert.service";
import { TaskTemplateService } from "../task-template.service";
import { TaskEditorComponent } from "./task-editor.component";
import { StandaloneTask } from "src/app/model/standalone-task";

@Component({
    selector: 'minty-new-task',
    imports: [CommonModule, FormsModule, TaskEditorComponent],
    templateUrl: 'new-standalone-task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class NewStandaloneTaskComponent implements OnInit {

    taskTemplates: Map<string, Map<string, string>> = new Map();
    outputTaskTemplates: Map<string, Map<string, string>> = new Map();

    task: StandaloneTask = {
        id: 0,
        name: '',
        triggered: false,
        watchLocation: '',
        taskTemplate: {
            name: '',
            configuration: new Map()
        },
        outputTemplate: {
            name: '',
            configuration: new Map()
        }
    };

    constructor(private alertService: AlertService,
        private router: Router,
        private taskService: TaskService,
        private taskTemplateService: TaskTemplateService) {
    }

    ngOnInit() {
        this.taskTemplateService.listTemplates().subscribe((taskTemplates: any) => {
            this.taskTemplates = taskTemplates as Map<string, Map<string, string>>;
        });
        this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: any) => {
            this.outputTaskTemplates = outputTaskTemplates as Map<string, Map<string, string>>;
        });
    }

    createTask() {
        if (this.task.triggered) {
            this.taskService.newTriggeredTask(this.task).subscribe(() => {
                this.alertService.postSuccess('Task Created!');
                this.router.navigateByUrl('task');
            });
        }
        else {
            this.taskService.newTask(this.task).subscribe(() => {
                this.alertService.postSuccess('Task Created!');
                this.router.navigateByUrl('task');
            });
        }
    }

    cancel() {
        this.router.navigateByUrl('/task');
    }

}
