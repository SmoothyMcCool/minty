package tom.api.services.assistant;

import tom.api.model.assistant.Assistant;

public interface AssistantRegistryService {

	Assistant createDefaultAssistant();

	Assistant createConversationNamingAssistant();

	Assistant createDiagrammingAssistant();

	Assistant createSummarizingAssistant();

	Assistant createMermaidConversionAssistant();

}
