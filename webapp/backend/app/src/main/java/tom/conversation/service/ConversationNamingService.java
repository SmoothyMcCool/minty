package tom.conversation.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.config.ExternalProperties;
import tom.conversation.model.Conversation;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.ollama.service.OllamaService;

@Service
public class ConversationNamingService {

	private static final Logger logger = LogManager.getLogger(ConversationNamingService.class);

	private final String conversationNamingModel;

	private final ConversationRepository conversationRepository;
	private final AssistantQueryService assistantQueryService;
	private final OllamaService ollamaService;
	private final ConversationServiceInternal conversationService;

	public ConversationNamingService(ConversationRepository conversationRepository,
			AssistantQueryService assistantQueryService, ConversationServiceInternal conversationService,
			OllamaService ollamaService, ExternalProperties properties) {
		this.conversationRepository = conversationRepository;
		this.assistantQueryService = assistantQueryService;
		this.ollamaService = ollamaService;
		this.conversationService = conversationService;
		conversationNamingModel = properties.get("conversationNamingModel");
	}

	@Scheduled(fixedDelay = 5000)
	@Transactional
	void nameConversations() {
		Assistant.CreateConversationNamingAssistant(conversationNamingModel);
		List<ConversationEntity> conversations = conversationRepository.findAllByTitle(null);

		// Ignore all conversations internal to workflows.
		conversations = conversations.stream().filter(conversation -> conversation
				.getAssociatedAssistantId() != AssistantManagementService.DefaultAssistantId).toList();

		conversations.forEach(conversation -> {
			List<Message> messages = ollamaService.getChatMemory().get(conversation.getConversationId().toString());
			if (messages.size() > 1) {
				logger.info("Starting on conversation ID " + conversation.getConversationId().toString());
				StringBuilder sb = new StringBuilder();
				messages.forEach(message -> {
					String speaker = message.getMessageType().getValue();
					String content = message.getText();
					sb.append(speaker + ": " + content);
				});

				AssistantQuery assistantQuery = new AssistantQuery();
				assistantQuery.setAssistantId(AssistantManagementService.ConversationNamingAssistantId);
				Conversation namingConversation = conversationService.newConversation(UserService.DefaultId,
						AssistantManagementService.ConversationNamingAssistantId);
				assistantQuery.setConversationId(namingConversation.getConversationId());
				assistantQuery.setQuery(sb.toString());

				try {

					String summary = "";

					UUID requestId = null;
					while (true) {
						try {
							logger.info(
									"asking assistantquery service for " + conversation.getConversationId().toString());
							requestId = assistantQueryService.ask(UserService.DefaultId, assistantQuery);
							logger.info("requestId: " + requestId);
							break;

						} catch (QueueFullException e) {
							Thread.sleep(Duration.ofSeconds(5));
						}
					}

					while (true) {
						logger.info("getting result for  " + requestId);
						StringResult llmResult = (StringResult) assistantQueryService.getResultFor(requestId);
						if (llmResult != null && llmResult.isComplete()) {
							summary = llmResult instanceof StringResult ? ((StringResult) llmResult).getValue() : "";
							break;
						}
						Thread.sleep(Duration.ofSeconds(5));
					}

					// In case we're using Qwen3, strip off the <think> block.
					if (summary.startsWith("<think>")) {
						summary = summary.substring(summary.indexOf("</think>") + "</think>".length());
					}
					summary = summary.trim();

					conversation.setTitle(summary);
					logger.info("Setting conversation title to " + summary);

					conversationRepository.save(conversation);

					conversationService.deleteConversation(namingConversation.getOwnerId(),
							namingConversation.getConversationId());

				} catch (InterruptedException e) {
					logger.warn("Thread was interrupted while sleeping and waiting for my turn with the LLM!");
				}

			}
		});
	}
}
