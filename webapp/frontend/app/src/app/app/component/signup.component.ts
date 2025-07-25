import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { User } from '../../model/user';
import { UserService } from '../../user.service';

@Component({
    selector: 'minty-signup',
    imports: [CommonModule, FormsModule],
    templateUrl: 'signup.component.html',
    styleUrls: ['../../global.css', './signup.component.css']
})
export class SignupComponent {
    user: User;
    repeatPassword = '';
    passwordMismatch = true;
    messages: string[] = [];

    constructor(private userService: UserService, private router: Router) {
        this.user = {
            name: '',
            password: '',
            externalAccount: '',
            externalPassword: ''
        };
    }

    signup(): boolean {
        this.messages = [];
        if (this.formValid()) {
            this.userService.signup(this.user)
                .subscribe({
                    next: () => {
                        this.userService.login(this.user.name, this.user.password)
                            .subscribe({
                                next: () => {
                                    this.router.navigateByUrl('/home');
                                    return true;
                                },
                                error: (error) => this.messages = error
                            });
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
