package tom.task.services;

import tom.task.services.assistant.AssistantManagementService;
import tom.task.services.assistant.AssistantQueryService;

public interface TaskServices {

	AssistantManagementService getAssistantManagementService();

	AssistantQueryService getAssistantQueryService();

	ConversationService getConversationService();

	HttpService getHttpService();

	PythonService getPythonService();

	RenderService getRenderService();

	UserService getUserService();
}
