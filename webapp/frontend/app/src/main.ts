import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app/component/app.component';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { XhrInterceptor } from './app/xhr.interceptor';
import { provideRouter } from '@angular/router';
import { ViewAssistantsComponent } from './app/assistant/component/view-assistants.component';
import { AuthorizedInterceptor } from './app/auth.interceptor';
import { LoginComponent } from './app/app/component/login.component';
import { SignupComponent } from './app/app/component/signup.component';
import { ViewMainComponent } from './app/app/component/view-main.component';
import { ViewUserComponent } from './app/app/component/view-user.component';
import { ViewConversationComponent } from './app/assistant/component/view-conversation.component';
import { AssistantsListComponent } from './app/assistant/component/assistants-list.component';
import { NewTaskComponent } from './app/task/component/new-task.component';
import { ViewTaskComponent } from './app/task/component/view-task.component';
import { TaskListComponent } from './app/task/component/task-list.component';
import { TaskComponent } from './app/task/component/task.component';
import { NewAssistantComponent } from './app/assistant/component/new-assistant.component';
import { ViewStatisticsComponent } from './app/app/component/view-statistics.component';

bootstrapApplication(AppComponent, {
  providers: [
        { provide: HTTP_INTERCEPTORS, useClass: AuthorizedInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: XhrInterceptor, multi: true },
        provideRouter([
            {
                path: '',
                redirectTo: 'home',
                pathMatch: 'full'
            },
            {
                path: 'login',
                component: LoginComponent,
            },
            {
                path: 'signup',
                component: SignupComponent,
            },
            {
                path: 'home',
                component: ViewMainComponent,
            },
            {
                path: 'home/**',
                redirectTo: 'home'
            },
            {
                path: 'assistants',
                component: ViewAssistantsComponent,
                children: [
                    {
                        path: 'new',
                        component: NewAssistantComponent
                    },
                    {
                        path: '',
                        component: AssistantsListComponent
                    }
                ]
            },
            {
                path: 'conversation/:id',
                component: ViewConversationComponent
            },
            {
                path: 'task',
                component: ViewTaskComponent,
                children: [
                    {
                        path: 'new',
                        component: NewTaskComponent
                    },
                    {
                        path: ':id',
                        component: TaskComponent
                    },
                    {
                        path: '',
                        component: TaskListComponent
                    }
                ]
            },
            {
                path: 'user',
                component: ViewUserComponent,
            },
            {
                path: 'statistics',
                component: ViewStatisticsComponent,
            }
        ]/*, withDebugTracing()*/),
        provideHttpClient(withInterceptorsFromDi())
    ] });
