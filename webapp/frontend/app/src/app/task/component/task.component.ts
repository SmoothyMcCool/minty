import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TaskService } from '../task.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Task } from "../../model/task";
import { TaskConfigComponent } from './task-config.component';
import { AlertService } from 'src/app/alert.service';

@Component({
    selector: 'ai-task',
    imports: [CommonModule, FormsModule, TaskConfigComponent],
    templateUrl: 'task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class TaskComponent implements OnInit {

    task: Task = {
        id: 0,
        template: '',
        name: '',
        description: '',
        defaultConfig: new Map<string, string>(),
        outputTask: '',
        outputTaskConfig: new Map<string, string>(),
    };
    taskConfig: Map<string, string> = new Map();
    taskConfigParams: Map<string, string> = new Map();
    outputTaskConfig: Map<string, string>;
    outputTaskConfigParams: Map<string, string> = new Map();

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private TaskService: TaskService,
        private alertService: AlertService) {
    }

    ngOnInit(): void {
        const taskId = Number(this.route.snapshot.paramMap.get('id'));

        this.TaskService.getTask(taskId).subscribe((task: Task) => {
            this.task = task;
            this.taskConfig = this.task.defaultConfig;
            this.outputTaskConfig = this.task.outputTaskConfig;

            this.TaskService.getTaskConfig(taskId).subscribe((taskConfigParams: Map<string, string>) => {
                this.taskConfigParams = taskConfigParams;
            })
            this.TaskService.getOutputConfig(taskId).subscribe((outputTaskConfigParams: Map<string, string>) => {
                this.outputTaskConfigParams = outputTaskConfigParams;
            })
        });
    }

    submit() {
        this.TaskService.execute(this.task.template, this.taskConfig, this.task.outputTask, this.outputTaskConfig).subscribe((result: string) => {
            this.alertService.postSuccess(result);
        });
        this.router.navigateByUrl('task');
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
