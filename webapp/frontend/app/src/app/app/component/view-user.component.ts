import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AlertService } from '../../alert.service';
import { User } from '../../model/user';
import { UserService } from '../../user.service';

@Component({
    selector: 'minty-user',
    imports: [CommonModule, FormsModule],
    templateUrl: 'view-user.component.html',
    styleUrls: ['./view-user.component.css']
})
export class ViewUserComponent {
    user: User;
    repeatPassword = '';
    passwordMismatch = true;
    messages: string[] = [];

    constructor(private userService: UserService, private router: Router, private alertService: AlertService) {
        this.user = this.userService.getUser();
    }

    update(): boolean {
        this.messages = [];
        if (this.formValid()) {
            this.userService.update(this.user)
                .subscribe({
                    next: () => {
                        this.user = this.userService.getUser();
                        this.alertService.postSuccess("Ok I updated you huzzah.")
                        return true;
                    },
                    error: (error: string[]) => this.messages = error
                });
            return false;
        } else {
            this.messages = [ 'Hey! There\'s invalid crap!.' ];
            return false;
        }
    }

    passwordUpdated(): void {
        this.passwordMismatch = this.user.password !== this.repeatPassword || this.user.password.length < 8;
    }

    formValid(): boolean {
        return !(this.passwordMismatch || this.user.name.length === 0);
    }
}
