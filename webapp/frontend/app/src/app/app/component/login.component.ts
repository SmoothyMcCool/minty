import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserService } from '../../user.service';

@Component({
    selector: 'ai-login',
    imports: [CommonModule, FormsModule],
    templateUrl: 'login.component.html',
    styleUrls: ['../../global.css', './login.component.css']
})

export class LoginComponent {

    loginFailed = false;
    credentials = {
        account: '',
        password: ''
    };

    constructor(private userService: UserService,
        private router: Router) {
    }

    login(): boolean {
        const token = sessionStorage.clear();
        this.loginFailed = false;
        this.userService.login(this.credentials.account, this.credentials.password)
            .subscribe({
                next: () => {
                    this.router.navigateByUrl('/home');
                },
                error: () => {
                    this.loginFailed = true;
                }
            });
        return false;
    }

    signup(): void {
        this.router.navigateByUrl('/signup');
    }

}
