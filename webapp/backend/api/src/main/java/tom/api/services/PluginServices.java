package tom.api.services;

import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.cache.CacheService;
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

	DocumentExtractorService getDocumentExtractorService();

	ProjectService getProjectService();

	KnowledgeService getKnowledgeService();

	CacheService getCacheService();

	SkillService getSkillsService();

	WorkflowService getWorkflowService();

	AgentRegistry getAgentRegistry();

}
