package tom.task.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import tom.api.services.ConversationService;
import tom.api.services.HttpService;
import tom.api.services.PluginServices;
import tom.api.services.ProjectService;
import tom.api.services.RenderService;
import tom.api.services.SkillService;
import tom.api.services.UserService;
import tom.api.services.WorkflowService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.cache.CacheService;
import tom.api.services.document.DocumentService;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.api.services.python.PythonService;
import tom.conversation.service.ConversationServiceInternal;

@Service
public class PluginServicesImpl implements PluginServices {

	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final ConversationServiceInternal conversationService;
	private final HttpService httpService;
	private final PythonService pythonService;
	private final RenderService renderService;
	private final UserService userService;
	private final DocumentService documentService;
	private final ProjectService projectService;
	private final CacheService cacheService;
	private final DocumentExtractorService documentExtractorService;
	private final SkillService skillsService;
	private final WorkflowService workflowService;

	public PluginServicesImpl(@Lazy AssistantManagementService assistantManagementService,
			@Lazy AssistantQueryService assistantQueryService, @Lazy ConversationServiceInternal conversationService,
			@Lazy HttpService httpService, @Lazy PythonService pythonService, @Lazy RenderService renderService,
			@Lazy UserService userService, @Lazy DocumentService documentService, @Lazy ProjectService projectService,
			@Lazy CacheService cacheService, @Lazy DocumentExtractorService documentExtractorService,
			@Lazy SkillService skillsService, @Lazy WorkflowService workflowService) {
		this.assistantManagementService = assistantManagementService;
		this.assistantQueryService = assistantQueryService;
		this.conversationService = conversationService;
		this.httpService = httpService;
		this.pythonService = pythonService;
		this.renderService = renderService;
		this.userService = userService;
		this.documentService = documentService;
		this.projectService = projectService;
		this.cacheService = cacheService;
		this.documentExtractorService = documentExtractorService;
		this.skillsService = skillsService;
		this.workflowService = workflowService;
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

	@Override
	public DocumentService getDocumentService() {
		return documentService;
	}

	@Override
	public ProjectService getProjectService() {
		return projectService;
	}

	@Override
	public CacheService getCacheService() {
		return cacheService;
	}

	@Override
	public DocumentExtractorService getDocumentExtractorService() {
		return documentExtractorService;
	}

	@Override
	public SkillService getSkillsService() {
		return skillsService;
	}

	@Override
	public WorkflowService getWorkflowService() {
		return workflowService;
	}
}
