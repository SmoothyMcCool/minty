import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Assistant, AssistantState } from '../../model/assistant';
import { AssistantService } from '../../assistant.service';
import { Router, RouterModule } from '@angular/router';
import { concatAll, mergeMap, of } from 'rxjs';
import { DocumentService } from '../../document.service';
import { FilterPipe } from '../../pipe/filter-pipe';

@Component({
    selector: 'minty-new-assistant',
    imports: [CommonModule, FormsModule, RouterModule, FilterPipe],
    templateUrl: 'new-assistant.component.html'
})
export class NewAssistantComponent implements OnInit {

    fileList: File[] = [];
    models: string[] = [];
    workingAssistant: Assistant = {
            id: 0,
            name: '',
            prompt: '',
            numFiles: 0,
            model: '',
            state: AssistantState.READY,
            shared: false
        };

    constructor(
        private assistantService: AssistantService,
        private documentService: DocumentService,
        private router: Router) {
    }

    ngOnInit(): void {
        this.assistantService.models().subscribe((models: string[]) => {
            this.models = models;
        });
    }

    formInvalid(): boolean {
        return this.workingAssistant.name.length === 0 || this.workingAssistant.model.length === 0;
    }

    createAssistant() {
        if (this.fileList !== null && this.fileList.length > 0) {
            this.workingAssistant.numFiles = this.fileList.length;
        }
        this.assistantService.create(this.workingAssistant).subscribe((assistant: Assistant) => {

            // Upload each file and attach it to the assistant we just made.

            if (this.fileList !== null && this.fileList.length > 0) {
                of(this.fileList).pipe(
                    concatAll(),
                    mergeMap((file: File) => {
                        return this.documentService.upload(assistant.id, file);
                    })
                ).subscribe(() => {
                    this.navigateTo('assistants');
                });
            }
            else {
                this.navigateTo('assistants');
            }
        });
    }

    fileListChanged(event: Event) {
        const newFiles = (event.target as HTMLInputElement).files;
        if (newFiles !== null) {
            this.fileList = Array.from(newFiles).concat(Array.from(this.fileList));
            this.fileList = [...new Set(this.fileList)];
        }
    }

    removeFile(filename: string) {
        this.fileList = this.fileList.filter(element => element.name != filename);
    }

    navigateTo(url: string): void {
        this.router.navigateByUrl(url);
    }
}
