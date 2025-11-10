package tom.task.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import tom.api.services.ConversationService;
import tom.api.services.HttpService;
import tom.api.services.RenderService;
import tom.api.services.TaskServices;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.python.PythonService;
import tom.conversation.service.ConversationServiceInternal;

@Service
public class TaskServicesImpl implements TaskServices {

	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final ConversationServiceInternal conversationService;
	private final HttpService httpService;
	private final PythonService pythonService;
	private final RenderService renderService;
	private final UserService userService;

	public TaskServicesImpl(@Lazy AssistantManagementService assistantManagementService,
			@Lazy AssistantQueryService assistantQueryService, @Lazy ConversationServiceInternal conversationService,
			@Lazy HttpService httpService, @Lazy PythonService pythonService, @Lazy RenderService renderService,
			@Lazy UserService userService) {
		this.assistantManagementService = assistantManagementService;
		this.assistantQueryService = assistantQueryService;
		this.conversationService = conversationService;
		this.httpService = httpService;
		this.pythonService = pythonService;
		this.renderService = renderService;
		this.userService = userService;
	}

	@Override
	public AssistantManagementService getAssistantManagementService() {
		return assistantManagementService;
	}

	@Override
	public AssistantQueryService getAssistantQueryService() {
		return assistantQueryService;
	}

	@Override
	public ConversationService getConversationService() {
		return conversationService;
	}

	@Override
	public HttpService getHttpService() {
		return httpService;
	}

	@Override
	public PythonService getPythonService() {
		return pythonService;
	}

	@Override
	public RenderService getRenderService() {
		return renderService;
	}

	@Override
	public UserService getUserService() {
		return userService;
	}

}
