package tom.assistant.service.management;

import java.io.IOException;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import tom.api.model.assistant.Assistant;
import tom.api.services.assistant.AssistantRegistryService;
import tom.config.MintyConfiguration;

@Service
public class AssistantRegistryServiceImpl implements AssistantRegistryService {

	private final AssistantRegistry assistantRegistry;

	public AssistantRegistryServiceImpl(MintyConfiguration properties)
			throws StreamReadException, DatabindException, IOException {
		assistantRegistry = new AssistantRegistry();
	}

	@Override
	public Assistant createDefaultAssistant(String model) {
		return assistantRegistry.get("default", model);
	}

	@Override
	public Assistant createConversationNamingAssistant(String model) {
		return assistantRegistry.get("Conversation Naming Bot", model);
	}

	@Override
	public Assistant createDiagrammingAssistant(String model) {
		return assistantRegistry.get("Diagramming Bot", model);
	}

}
