package tom.api.services;

import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.python.PythonService;

public interface TaskServices {

	AssistantManagementService getAssistantManagementService();

	AssistantQueryService getAssistantQueryService();

	ConversationService getConversationService();

	HttpService getHttpService();

	PythonService getPythonService();

	RenderService getRenderService();

	UserService getUserService();

	DocumentService getDocumentService();
}
