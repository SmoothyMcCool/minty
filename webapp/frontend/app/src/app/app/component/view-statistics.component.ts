import { Component } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { UserMeta } from '../../model/user-meta';
import { MetadataService } from '../../metadata.service';
import { UserService } from 'src/app/user.service';
import { DisplayMode, User } from 'src/app/model/user';

@Component({
	selector: 'minty-view-statistics',
	imports: [CommonModule, DatePipe],
	templateUrl: 'view-statistics.component.html',
	styleUrls: ['./view-statistics.component.css']
})
export class ViewStatisticsComponent {
	metadata: UserMeta[];
	user: User;
	DisplayMode = DisplayMode;

	constructor(private metadataService: MetadataService, private userService: UserService) {
		this.metadataService.getMetadata().subscribe(metadata => this.metadata = metadata);
		this.user = this.userService.getUser();
	}
}
