package tom.conversation.service;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.config.ExternalProperties;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.model.Assistant;
import tom.ollama.service.OllamaService;

@Service
public class ConversationNamingService {

	private static final Logger logger = LogManager.getLogger(ConversationNamingService.class);

	private final String conversationNamingModel;

	private final ConversationRepository conversationRepository;
	private final AssistantQueryService assistantQueryService;
	private final OllamaService ollamaService;

	public ConversationNamingService(ConversationRepository conversationRepository,
			AssistantQueryService assistantQueryService, OllamaService ollamaService,
			ExternalProperties properties) {
		this.conversationRepository = conversationRepository;
		this.assistantQueryService = assistantQueryService;
		this.ollamaService = ollamaService;
		conversationNamingModel = properties.get("conversationNamingModel");
	}

	@Scheduled(fixedDelay = 5000)
	@Transactional
	void nameConversations() {

		Assistant namingAssistant = Assistant.CreateConversationNamingAssistant(conversationNamingModel);
		List<ConversationEntity> conversations = conversationRepository.findAllByTitle(null);

		// Ignore all conversations internal to workflows.
		conversations = conversations.stream()
				.filter(conversation -> conversation
						.getAssociatedAssistantId() != AssistantManagementService.DefaultAssistantId)
				.toList();

		conversations.forEach(conversation -> {
			List<Message> messages = ollamaService.getChatMemory().get(conversation.getConversationId().toString());
			if (messages.size() > 1) {
				StringBuilder sb = new StringBuilder();
				messages.forEach(message -> {
					String speaker = message.getMessageType().getValue();
					String content = message.getText();
					sb.append(speaker + ": " + content);
				});
				String summary = assistantQueryService.ask(namingAssistant, sb.toString());

				// In case we're using Qwen3, strip off the <think> block.
				if (summary.startsWith("<think>")) {
					summary = summary.substring(summary.indexOf("</think>") + "</think>".length());
				}
				summary = summary.trim();

				conversation.setTitle(summary);
				logger.info("Setting conversation title to " + summary);

				conversationRepository.save(conversation);
			}
		});
	}
}
