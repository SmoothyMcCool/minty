import { Component, forwardRef, Input, OnInit } from "@angular/core";
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { AssistantService } from "src/app/assistant.service";
import { Assistant } from "src/app/model/assistant";

@Component({
    selector: 'ai-workflow-config',
    templateUrl: 'workflow-config.component.html',
    imports: [CommonModule, FormsModule],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => WorkflowConfigComponent),
            multi: true
        }
    ],
    //styleUrls: []
})
export class WorkflowConfigComponent implements OnInit, ControlValueAccessor {

    config: Map<string, string> = new Map();
    onChange: any = () => {};
    onTouched: any = () => {};
    touched = false;
    disabled = false;

    assistants: Assistant[] = [];

    private _configParams: Map<string, string>;
    @Input()
    set configParams(value: Map<string, string>){
        this._configParams = value;
    }
    get configParams(): Map<string, string> {
        return this._configParams;
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
        if (this.config !== undefined) {
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