import { CommonModule } from "@angular/common";
import { Component, forwardRef, Input } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { Task } from "../../model/task";
import { TaskConfigurationEditorComponent } from "./task-configuration-editor.component";

@Component({
    selector: 'minty-task-editor',
    imports: [CommonModule, FormsModule, TaskConfigurationEditorComponent],
    templateUrl: 'task-editor.component.html',
    styleUrls: ['../../global.css', 'task.component.css'],
    providers: [{
        provide: NG_VALUE_ACCESSOR,
        useExisting: forwardRef(() => TaskEditorComponent),
        multi: true
    }]
})
export class TaskEditorComponent implements ControlValueAccessor {

    @Input() name: string;

    private _taskTemplates: Map<string, Map<string, string>> ;
    @Input()
    set taskTemplates(value: Map<string, Map<string, string>> ){
        this._taskTemplates = value;
        if (this.task?.name) {
            this.taskChanged(this.task.name);
        }
    }
    get taskTemplates(): Map<string, Map<string, string>>  {
        return this._taskTemplates;
    }

    task: Task = {
        name: '',
        configuration: new Map<string, string>()
    };

    isFileTriggered: boolean = false;
    triggerDirectory: string = '';

    configurationDefinition = new Map<string, string>();

    onChange: any = (value: any) => {};
    onTouched: any = () => {};

    constructor() {
    }

    writeValue(obj: any): void {
        this.task = obj;
        if (this.task?.name) {
            this.taskChanged(this.task.name);
        }
    }
    registerOnChange(fn: any): void {
        this.onChange = fn;
    }
    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }
    setDisabledState(isDisabled: boolean): void {
        // Ignored.
    }

    taskChanged($event) {
        this.task.name = $event;
        this.configurationDefinition = new Map(this.taskTemplates.get($event));

        /*this.task.configuration = new Map<string, string>();

        if (this.configurationDefinition !== undefined) {
            this.task.configuration = new Map(this.configurationDefinition);
        }
        else {
            this.task.configuration = new Map<string, string>();
        }*/
    }
}
