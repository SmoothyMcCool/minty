package tom.task.service;

import org.springframework.stereotype.Service;

import tom.conversation.service.ConversationServiceInternal;
import tom.task.services.ConversationService;
import tom.task.services.HttpService;
import tom.task.services.PythonService;
import tom.task.services.RenderService;
import tom.task.services.TaskServices;
import tom.task.services.UserService;
import tom.task.services.assistant.AssistantManagementService;
import tom.task.services.assistant.AssistantQueryService;

@Service
public class TaskServicesImpl implements TaskServices {

	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final ConversationServiceInternal conversationService;
	private final HttpService httpService;
	private final PythonService pythonService;
	private final RenderService renderService;
	private final UserService userService;

	public TaskServicesImpl(AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, ConversationServiceInternal conversationService,
			HttpService httpService, PythonService pythonService, RenderService renderService,
			UserService userService) {
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
