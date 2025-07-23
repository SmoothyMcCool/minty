import { CommonModule } from "@angular/common";
import { Component, OnInit } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { Task } from "../../model/task";
import { TaskService } from "../task.service";
import { Router } from "@angular/router";
import { TaskConfigComponent } from "./task-config.component";
import { AlertService } from "src/app/alert.service";

@Component({
    selector: 'ai-new--task',
    imports: [CommonModule, FormsModule, TaskConfigComponent],
    templateUrl: 'new-task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class NewTaskComponent implements OnInit {

    taskTemplates: Map<string, Map<string, string>> = new Map();
    outputTaskTemplates: Map<string, Map<string, string>> = new Map();

    task: Task = {
        id: 0,
        template: '',
        name: '',
        description: '',
        defaultConfig: new Map<string, string>(),
        outputTask: '',
        outputTaskConfig: new Map<string, string>()
    };
    isFileTriggered: boolean = false;
    triggerDirectory: string = '';

    configParams = new Map<string, string>();
    outputTaskConfigParams = new Map<string, string>();

    constructor(private alertService: AlertService,
        private router: Router,
        private TaskService: TaskService) {
    }

    ngOnInit() {
        this.TaskService.listTemplates().subscribe((taskTemplates: any) => {
            this.taskTemplates = taskTemplates as Map<string, Map<string, string>>;
        });
        this.TaskService.listOutputTasks().subscribe((outputTaskTemplates: any) => {
            this.outputTaskTemplates = outputTaskTemplates as Map<string, Map<string, string>>;
        });
    }

    formInvalid(): boolean {
        return this.task.name.length === 0 || this.task.template.length === 0;
    }

    createTask() {
        if (this.isFileTriggered) {
            this.TaskService.newTriggeredTask(this.task, this.triggerDirectory).subscribe(() => {
                this.alertService.postSuccess('Task Created!');
                this.router.navigateByUrl('task');
            });
        }
        else {
            this.TaskService.newTask(this.task).subscribe(() => {
                this.alertService.postSuccess('Task Created!');
                this.router.navigateByUrl('task');
            });
        }
    }

    taskChanged($event) {
        this.configParams = new Map(this.taskTemplates.get($event));

        this.task.defaultConfig = new Map<string, string>();

        if (this.configParams !== undefined) {
            this.task.defaultConfig = new Map(this.configParams);
        }
        else {
            this.task.defaultConfig = new Map<string, string>();
        }
    }

    outputTaskChanged($event) {
        this.outputTaskConfigParams = new Map(this.outputTaskTemplates.get($event));

        this.task.outputTaskConfig = new Map<string, string>();

        if (this.outputTaskConfigParams !== undefined) {
            this.task.outputTaskConfig = new Map(this.outputTaskConfigParams);
        }
        else {
            this.task.outputTaskConfig = new Map<string, string>();
        }
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
