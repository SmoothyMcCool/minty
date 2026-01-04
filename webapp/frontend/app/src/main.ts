import { bootstrapApplication } from '@angular/platform-browser';
import { provideAnimations } from '@angular/platform-browser/animations';
import { AppComponent } from './app/app/component/app.component';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { XhrInterceptor } from './app/xhr.interceptor';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
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
import { WorkflowListComponent } from './app/workflow/component/workflow-list.component';
import { ClipboardOptions, MERMAID_OPTIONS, provideMarkdown } from 'ngx-markdown';
import { EditAssistantComponent } from './app/assistant/component/edit-assistant.component';
import { ViewDocumentsComponent } from './app/document/view-documents.component';
import { ResponseInterceptor } from './app/response-interceptor';
import { ViewDiagramsComponent } from './app/diagram/component/view-diagrams.component';
import { EditWorkflowComponent } from './app/workflow/component/edit-workflow.component';
import 'bootstrap/dist/js/bootstrap.bundle.min.js';
import { NewWorkflowComponent } from './app/workflow/component/new-workflow.component';
import { RunWorkflowComponent } from './app/workflow/component/run-workflow.component';
import { ViewHelpComponent } from './app/help/component/view-help.component';
import { DocumentsHelpComponent } from './app/help/component/documents-help.component';
import { AssistantsHelpComponent } from './app/help/component/assistants-help.component';
import { AssistantsHelpEditingComponent } from './app/help/component/assistants-help-editing.component';
import { AssistantsHelpPremadeComponent } from './app/help/component/assistants-help-premade.component';
import { AssistantsHelpIdeasComponent } from './app/help/component/assistants-help-ideas.component';
import { WorkflowsHelpComponent } from './app/help/component/workflows-help.component';
import { WorkflowsPugHelpComponent } from './app/help/component/workflows-pug-help.component';
import { WorkflowsTasksHelpComponent } from './app/help/component/workflows-tasks-help.component';
import { WorkflowsEditingHelpComponent } from './app/help/component/workflows-editing-help.component';
import { WorkflowsTasksCreateComponent } from './app/help/component/workflows-tasks-create.component';
import { ViewProjectComponent } from './app/project/component/view-project.component';
import { ProjectListComponent } from './app/project/component/project-list.component';
import { EditProjectComponent } from './app/project/component/edit-project.component';

export function clipboardOptionsFactory(): ClipboardOptions {
	return {};
}

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
				path: 'assistants',
				component: ViewAssistantsComponent,
				children: [
					{
						path: '',
						component: AssistantsListComponent
					},
					{
						path: 'edit/:id',
						component: EditAssistantComponent
					},
					{
						path: 'new',
						component: NewAssistantComponent
					}
				]
			},
			{
				path: 'conversation/:id',
				component: ViewConversationComponent
			},
			{
				path: 'diagrams',
				component: ViewDiagramsComponent
			},
			{
				path: 'documents',
				component: ViewDocumentsComponent
			},
			{
				path: 'help',
				component: ViewHelpComponent,
				children: [
					{
						path: 'assistants',
						component: AssistantsHelpComponent,
						children: [
							{
								path: 'edit',
								component: AssistantsHelpEditingComponent
							},
							{
								path: 'ideas',
								component: AssistantsHelpIdeasComponent
							},
							{
								path: 'premade',
								component: AssistantsHelpPremadeComponent
							}
						]
					},
					{
						path: 'documents',
						component: DocumentsHelpComponent
					},
					{
						path: 'workflows',
						component: WorkflowsHelpComponent,
						children: [
							{
								path: 'custom',
								component: WorkflowsTasksCreateComponent
							},{
								path: 'editing',
								component: WorkflowsEditingHelpComponent
							},
							{
								path: 'pug',
								component: WorkflowsPugHelpComponent
							},
							{
								path: 'tasks',
								component: WorkflowsTasksHelpComponent
							}
						]
					}
					
				]
			},
			{
				path: 'login',
				component: LoginComponent,
			},
			{
				path: 'projects',
				component: ViewProjectComponent,
				children: [
					{
						path: '',
						component: ProjectListComponent
					},
					{
						path: ':id',
						component: EditProjectComponent
					},
				]
			},
			{
				path: 'signup',
				component: SignupComponent,
			},
			{
				path: 'statistics',
				component: ViewStatisticsComponent
			},
			{
				path: 'user',
				component: ViewUserComponent
			},
			{
				path: 'workflow',
				component: ViewWorkflowComponent,
				children: [
					{
						path: '',
						component: WorkflowListComponent
					},
					{
						path: 'edit/:id',
						component: EditWorkflowComponent
					},
					{
						path: 'new',
						component: NewWorkflowComponent
					},
					{
						path: ':id',
						component: RunWorkflowComponent
					}
				]
			}
		],
		withInMemoryScrolling({
			anchorScrolling: 'enabled'
		})
		/*, withDebugTracing()*/),
		provideHttpClient(withInterceptorsFromDi()),
		provideMarkdown({
			mermaidOptions: {
				provide: MERMAID_OPTIONS,
				useValue: {
					darkMode: false,
					look: 'classic',
				}
			}
		}),
		provideAnimations()
	]
});
