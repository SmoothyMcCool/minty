package tom.api.services;

import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.cache.CacheService;
import tom.api.services.document.DocumentService;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.api.services.python.PythonService;

public interface PluginServices {

	AssistantManagementService getAssistantManagementService();

	AssistantQueryService getAssistantQueryService();

	ConversationService getConversationService();

	HttpService getHttpService();

	PythonService getPythonService();

	RenderService getRenderService();

	UserService getUserService();

	DocumentService getDocumentService();

	ProjectService getProjectService();

	CacheService getCacheService();

	DocumentExtractorService getDocumentExtractorService();

	SkillService getSkillsService();

	WorkflowService getWorkflowService();

	AgentRegistry getAgentRegistry();
}
