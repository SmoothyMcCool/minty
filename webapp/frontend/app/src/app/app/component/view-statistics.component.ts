import { Component, OnInit } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { UserMeta } from '../../model/user-meta';
import { MetadataService } from '../../metadata.service';
import { User } from '../../model/user';
import { UserService } from '../../user.service';

@Component({
	selector: 'minty-view-statistics',
	imports: [CommonModule, DatePipe],
	templateUrl: 'view-statistics.component.html'
})
export class ViewStatisticsComponent implements OnInit {
	metadata!: UserMeta[];
	user!: User;

	constructor(private metadataService: MetadataService, private userService: UserService) {
		this.metadataService.getMetadata().subscribe(metadata => this.metadata = metadata.sort((a, b) => b.lastLogin.valueOf() - a.lastLogin.valueOf()));
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
	}

	ngOnInit() {
		this.userService.getUser().subscribe(user => {
			this.user = user;
		});
	}
}
