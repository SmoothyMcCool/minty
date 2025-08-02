import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { UserMeta } from '../../model/user-meta';
import { MetadataService } from '../../metadata.service';

@Component({
    selector: 'minty-view-statistics',
    imports: [CommonModule, DatePipe],
    templateUrl: 'view-statistics.component.html',
    styleUrls: ['./view-statistics.component.css']
})
export class ViewStatisticsComponent {
    metadata: UserMeta[];

    constructor(private metadataService: MetadataService) {
        this.metadataService.getMetadata().subscribe(metadata => this.metadata = metadata);
    }
}
