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
	public Assistant createDefaultAssistant() {
		return assistantRegistry.get("default");
	}

	@Override
	public Assistant createConversationNamingAssistant() {
		return assistantRegistry.get("Conversation Naming Bot");
	}

	@Override
	public Assistant createDiagrammingAssistant() {
		return assistantRegistry.get("Diagramming Bot");
	}

	@Override
	public Assistant createSummarizingAssistant() {
		return assistantRegistry.get("Summarizer Bot");
	}

	@Override
	public Assistant createMermaidConversionAssistant() {
		return assistantRegistry.get("EA to Mermaid Conversion Bot");
	}

	@Override
	public Assistant createConversationPlannerAssistant() {
		return assistantRegistry.get("Planner");
	}

	@Override
	public Assistant createConversationSynthesizerAssistant() {
		return assistantRegistry.get("Synthesizer");
	}
}
