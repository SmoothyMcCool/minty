import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TaskService } from '../task.service';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskConfigurationEditorComponent } from './task-configuration-editor.component';
import { AlertService } from 'src/app/alert.service';
import { TaskTemplateService } from '../task-template.service';
import { StandaloneTask } from 'src/app/model/standalone-task';

@Component({
    selector: 'minty-task',
    imports: [CommonModule, FormsModule, TaskConfigurationEditorComponent],
    templateUrl: 'task.component.html',
    styleUrls: ['../../global.css', 'task.component.css']
})
export class TaskComponent implements OnInit {

    task: StandaloneTask = {
        id: 0,
        name: '',
        triggered: false,
        taskTemplate: {
            name: '',
            configuration: new Map()
        },
        outputTemplate: {
            name: '',
            configuration: new Map()
        }
    };
    taskConfigurationDefinition: Map<string, string> = new Map();
    outputTaskConfigurationDefinition: Map<string, string> = new Map();

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private taskService: TaskService,
        private taskTemplateService: TaskTemplateService,
        private alertService: AlertService) {
    }

    ngOnInit(): void {
        const taskId = Number(this.route.snapshot.paramMap.get('id'));

        this.taskService.getTask(taskId).subscribe((task: StandaloneTask) => {
            

            this.taskTemplateService.getTemplateConfiguration(taskId).subscribe((taskConfigurationDefinition: Map<string, string>) => {
                this.taskConfigurationDefinition = taskConfigurationDefinition;
                this.taskTemplateService.getOutputTemplateConfiguration(taskId).subscribe((outputTaskConfigurationDefinition: Map<string, string>) => {
                    this.outputTaskConfigurationDefinition = outputTaskConfigurationDefinition;
                    this.task = task;
                })
            })
            
        });
    }

    submit() {
        this.taskService.execute(this.task).subscribe((result: string) => {
            this.alertService.postSuccess(result);
        });
        this.router.navigateByUrl('task');
    }

    navigateTo(url: string) {
        this.router.navigateByUrl(url);
    }
}
