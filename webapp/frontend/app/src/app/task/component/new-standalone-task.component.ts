import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { TaskService } from "../task.service";
import { Router } from "@angular/router";
import { AlertService } from "src/app/alert.service";
import { TaskTemplateService } from "../task-template.service";
import { TaskEditorComponent } from "./task-editor.component";
import { StandaloneTask } from "src/app/model/standalone-task";
import { TaskDescription } from "src/app/model/task-description";

@Component({
    selector: 'minty-new-task',
    imports: [CommonModule, FormsModule, TaskEditorComponent],
    templateUrl: 'new-standalone-task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class NewStandaloneTaskComponent implements OnInit {

    taskTemplates: TaskDescription[] = [];
    outputTaskTemplates: TaskDescription[] = [];

    task: StandaloneTask = {
        id: 0,
        ownerId: 0,
        name: '',
        shared: false,
        triggered: false,
        watchLocation: '',
        taskTemplate: {
            name: '',
            configuration: new Map<string, string>()
        },
        outputTemplate: {
            name: '',
            configuration: new Map<string, string>()
        }
    };

    constructor(private alertService: AlertService,
        private router: Router,
        private taskService: TaskService,
        private taskTemplateService: TaskTemplateService) {
    }

    ngOnInit() {
        this.taskTemplateService.listTemplates().subscribe((taskTemplates: TaskDescription[]) => {
            this.taskTemplates = taskTemplates;
        });
        this.taskTemplateService.listOutputTemplates().subscribe((outputTaskTemplates: TaskDescription[]) => {
            this.outputTaskTemplates = outputTaskTemplates;
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
