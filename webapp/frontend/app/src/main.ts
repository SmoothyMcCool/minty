import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app/component/app.component';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { XhrInterceptor } from './app/xhr.interceptor';
import { provideRouter } from '@angular/router';
import { ViewAssistantsComponent } from './app/assistant/component/view-assistants.component';
import { AuthorizedInterceptor } from './app/auth.interceptor';
import { LoginComponent } from './app/app/component/login.component';
import { SignupComponent } from './app/app/component/signup.component';
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
import { MarkdownModule, MERMAID_OPTIONS, provideMarkdown } from 'ngx-markdown';
import { EditAssistantComponent } from './app/assistant/component/edit-assistant.component';
import { EditWorkflowComponent } from './app/workflow/component/edit-workflow.component';
import { ViewDocumentsComponent } from './app/document/view-documents.component';
import { ResponseInterceptor } from './app/response-interceptor';
import { DisplayWorkflowResultComponent } from './app/workflow/component/display-workflow-result.component';

bootstrapApplication(AppComponent, {
  providers: [
		{ provide: HTTP_INTERCEPTORS, useClass: AuthorizedInterceptor, multi: true },
		{ provide: HTTP_INTERCEPTORS, useClass: XhrInterceptor, multi: true },
		{ provide: HTTP_INTERCEPTORS, useClass: ResponseInterceptor, multi: true },
		provideRouter([
			{
				path: '',
				redirectTo: 'assistants',
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
				path: 'assistants',
				component: ViewAssistantsComponent,
				children: [
					{
						path: 'new',
						component: NewAssistantComponent
					},
					{
						path: 'edit/:id',
						component: EditAssistantComponent
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
						path: 'result/:id',
						component: DisplayWorkflowResultComponent
					},
					{
						path: 'edit/:id',
						component: EditWorkflowComponent
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
				path: 'documents',
				component: ViewDocumentsComponent
			},
			{
				path: 'user',
				component: ViewUserComponent
			},
			{
				path: 'statistics',
				component: ViewStatisticsComponent
			}
		]/*, withDebugTracing()*/),
		provideHttpClient(withInterceptorsFromDi()),
		provideMarkdown({
			mermaidOptions: {
				provide: MERMAID_OPTIONS,
				useValue: {
					darkMode: false,
					look: 'classic',
				}
			}
		})
		//importProvidersFrom(
		//	MarkdownModule.forRoot()
		//)
	]
});
