package tom.api.services.assistant;

import tom.model.Assistant;

public interface AssistantRegistryService {

	Assistant createDefaultAssistant(String model);

	Assistant createConversationNamingAssistant(String model);

	Assistant createDiagrammingAssistant(String model);

}
