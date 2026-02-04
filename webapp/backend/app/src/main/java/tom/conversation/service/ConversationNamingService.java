package tom.conversation.service;

import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.ConversationId;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.model.conversation.Conversation;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.AssistantRegistryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.config.MintyConfiguration;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.ollama.service.OllamaService;

@Service
public class ConversationNamingService {

	private static final Logger logger = LogManager.getLogger(ConversationNamingService.class);

	private final String conversationNamingModel;

	private final ConversationRepository conversationRepository;
	private final AssistantQueryService assistantQueryService;
	private final AssistantRegistryService assistantRegistryService;
	private final OllamaService ollamaService;
	private final ConversationServiceInternal conversationService;

	public ConversationNamingService(ConversationRepository conversationRepository,
			AssistantQueryService assistantQueryService, AssistantRegistryService assistantRegistryService,
			ConversationServiceInternal conversationService, OllamaService ollamaService,
			MintyConfiguration properties) {
		this.conversationRepository = conversationRepository;
		this.assistantQueryService = assistantQueryService;
		this.assistantRegistryService = assistantRegistryService;
		this.ollamaService = ollamaService;
		this.conversationService = conversationService;
		conversationNamingModel = properties.getConfig().ollama().conversationNamingModel();
	}

	@Scheduled(fixedDelay = 5000)
	@Transactional
	void nameConversations() {
		assistantRegistryService.createConversationNamingAssistant(conversationNamingModel);
		List<ConversationEntity> conversations = conversationRepository.findAllByTitle(null);

		// Ignore all conversations internal to workflows.
		conversations = conversations.stream().filter(conversation -> conversation
				.getAssociatedAssistantId() != AssistantManagementService.DefaultAssistantId).toList();

		conversations.forEach(conversation -> {
			List<Message> messages = ollamaService.getChatMemory()
					.get(conversation.getConversationId().value().toString());
			if (messages.size() > 1) {
				logger.info("Starting on conversation ID " + conversation.getConversationId().toString());
				StringBuilder sb = new StringBuilder();
				messages.forEach(message -> {
					String speaker = message.getMessageType().getValue();
					String content = message.getText();
					sb.append(speaker + ": " + content);
				});

				AssistantQuery assistantQuery = new AssistantQuery();
				AssistantSpec assistantSpec = new AssistantSpec(
						AssistantManagementService.ConversationNamingAssistantId, null);
				assistantQuery.setAssistantSpec(assistantSpec);
				Conversation namingConversation = conversationService.newConversation(UserService.DefaultId,
						AssistantManagementService.ConversationNamingAssistantId);
				assistantQuery.setConversationId(namingConversation.getConversationId());
				assistantQuery.setQuery(sb.toString());

				try {

					String summary = "";

					ConversationId requestId = null;
					while (true) {
						try {
							logger.info(
									"asking assistantquery service for " + conversation.getConversationId().toString());
							requestId = assistantQueryService.ask(UserService.DefaultId, assistantQuery);
							logger.info("requestId: " + requestId);
							break;

						} catch (QueueFullException | ConversationInUseException e) {
							logger.warn(
									"Failed to enqueue request. Trying again in 5 seconds. Reason: " + e.toString());
							Thread.sleep(Duration.ofSeconds(5));
						}
					}

					while (true) {
						logger.info("getting result for  " + requestId);
						StringResult llmResult = (StringResult) assistantQueryService
								.getResultAndRemoveIfComplete(requestId);
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
					Thread.currentThread().interrupt();
					logger.warn("Thread was interrupted while sleeping and waiting for my turn with the LLM!");
					return;
				}

			}
		});
	}
}
