package tom.task.service;

import org.springframework.stereotype.Service;

import tom.task.services.AssistantService;
import tom.task.services.ConversationService;
import tom.task.services.DocumentService;
import tom.task.services.HttpService;
import tom.task.services.PythonService;
import tom.task.services.RenderService;
import tom.task.services.TaskServices;

@Service
public class TaskServicesImpl implements TaskServices {

	private final AssistantService assistantService;
	private final ConversationService conversationService;
	private final DocumentService documentService;
	private final HttpService httpService;
	private final PythonService pythonService;
	private final RenderService renderService;

	public TaskServicesImpl(AssistantService assistantService, ConversationService conversationService,
			DocumentService documentService, HttpService httpService, PythonService pythonService,
			RenderService renderService) {
		this.assistantService = assistantService;
		this.conversationService = conversationService;
		this.documentService = documentService;
		this.httpService = httpService;
		this.pythonService = pythonService;
		this.renderService = renderService;
	}

	@Override
	public AssistantService getAssistantService() {
		return assistantService;
	}

	@Override
	public ConversationService getConversationService() {
		return conversationService;
	}

	@Override
	public DocumentService getDocumentService() {
		return documentService;
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

}
