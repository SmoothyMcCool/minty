import { Component, forwardRef, Input, OnInit } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { AssistantService } from "src/app/assistant.service";
import { Assistant } from "src/app/model/assistant";

@Component({
    selector: 'minty-task-config-editor',
    templateUrl: 'task-configuration-editor.component.html',
    imports: [CommonModule, FormsModule],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => TaskConfigurationEditorComponent),
            multi: true
        }
    ]
})
export class TaskConfigurationEditorComponent implements OnInit, ControlValueAccessor {

    config: Map<string, string> = new Map();
    onChange: any = () => {};
    onTouched: any = () => {};
    touched = false;
    disabled = false;

    assistants: Assistant[] = [];

    private _configurationDefinition: Map<string, string>;
    @Input()
    set configurationDefinition(value: Map<string, string>){
        this._configurationDefinition = value;
    }
    get configurationDefinition(): Map<string, string> {
        return this._configurationDefinition;
    }

    constructor(private assistantService: AssistantService) {
    }

    ngOnInit(): void {
        this.assistantService.list().subscribe((assistants: Assistant[]) => {
            this.assistants = assistants;
        });
    }

    valueChanged(param: string, value: string) {
        this.config.set(param, value);
        this.onTouched();
        this.onChange(this.config);
    }

    getConfig(key: string) {
        if (this.config !== undefined && this.config.get !== undefined) {
            return this.config.get(key);
        }
        return null;
    }

    writeValue(obj: any): void {
        this.config = obj;
    }
    registerOnChange(fn: any): void {
        this.onChange = fn;
    }
    registerOnTouched(fn: any): void {
        this.onTouched = fn;
    }
    setDisabledState(isDisabled: boolean): void {
        // Nah.
    }
}