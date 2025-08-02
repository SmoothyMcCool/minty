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
import { NewAssistantComponent } from './app/assistant/component/new-assistant.component';
import { ViewStatisticsComponent } from './app/app/component/view-statistics.component';
import { ViewWorkflowComponent } from './app/workflow/component/view-workflow.component';
import { NewWorkflowComponent } from './app/workflow/component/new-workflow.component';
import { WorkflowComponent } from './app/workflow/component/workflow.component';
import { WorkflowListComponent } from './app/workflow/component/workflow-list.component';
import { importProvidersFrom } from '@angular/core';
import { MarkdownModule } from 'ngx-markdown';

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
                path: 'workflow',
                component: ViewWorkflowComponent,
                children: [
                    {
                        path: 'new',
                        component: NewWorkflowComponent
                    },
                    {
                        path: ':id',
                        component: WorkflowComponent
                    },
                    {
                        path: '',
                        component: WorkflowListComponent
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
        provideHttpClient(withInterceptorsFromDi()),
        importProvidersFrom(
            MarkdownModule.forRoot()
        )
    ]
});
