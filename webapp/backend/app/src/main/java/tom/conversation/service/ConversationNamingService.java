package tom.conversation.service;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.model.conversation.Conversation;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.assistant.service.management.AssistantRegistry;
import tom.config.MintyConfiguration;
import tom.conversation.model.ConversationEntity;
import tom.conversation.repository.ConversationRepository;
import tom.llm.service.LlmService;

@Service
public class ConversationNamingService {

	private static final Logger logger = LogManager.getLogger(ConversationNamingService.class);

	private final ConversationRepository conversationRepository;
	private final AssistantQueryService assistantQueryService;
	private final LlmService llmService;
	private final ConversationServiceInternal conversationService;

	public ConversationNamingService(ConversationRepository conversationRepository,
			AssistantQueryService assistantQueryService, AssistantRegistry assistantRegistry,
			ConversationServiceInternal conversationService, LlmService llmService, MintyConfiguration properties) {
		this.conversationRepository = conversationRepository;
		this.assistantQueryService = assistantQueryService;
		this.llmService = llmService;
		this.conversationService = conversationService;
	}

	@Scheduled(fixedDelay = 5000)
	@Transactional
	void nameConversations() {
		List<ConversationEntity> conversations = conversationRepository.findAllByTitle(null);

		conversations = conversations.stream().filter(conversation -> conversation
				.getAssociatedAssistantId() != AssistantManagementService.DefaultAssistantId).toList();

		conversations.forEach(conversation -> {
			List<Message> messages = llmService.getChatMemory()
					.get(conversation.getConversationId().value().toString());

			if (messages.size() > 1 || (messages.size() == 1 && messages.get(0).getText().length() > 80)) {
				logger.info("Starting on conversation ID " + conversation.getConversationId().value().toString());
				StringBuilder sb = new StringBuilder();
				messages.forEach(message -> {
					String speaker = message.getMessageType().getValue();
					String content = message.getText();
					sb.append(speaker + ": " + content);
				});

				AssistantQuery assistantQuery = new AssistantQuery();
				AssistantSpec assistantSpec = new AssistantSpec(
						AssistantManagementService.ConversationNamingAssistantId);
				assistantQuery.setAssistantSpec(assistantSpec);
				Conversation namingConversation = conversationService.newConversation(UserService.DefaultId,
						AssistantManagementService.ConversationNamingAssistantId);
				assistantQuery.setConversationId(namingConversation.getConversationId());
				assistantQuery.setQuery(sb.toString());

				String summary = null;
				while (summary == null) {
					try {
						// blocks this scheduled thread, which is acceptable here
						summary = assistantQueryService.ask(UserService.DefaultId, assistantQuery).get();
					} catch (CancellationException e) {
						logger.warn("Conversation naming request was cancelled.");
						return;
					} catch (ExecutionException e) {
						if (e.getCause() instanceof QueueFullException
								|| e.getCause() instanceof ConversationInUseException) {
							return; // Just return and we'll try next time the scheduler fires.
						} else {
							logger.warn("Conversation naming failed with unexpected error.", e.getCause());
							return;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						logger.warn("Thread was interrupted while waiting for conversation naming.");
						return;
					} catch (QueueFullException e) {
						return; // Just return and we'll try next time the scheduler fires.
					} catch (ConversationInUseException e) {
						return; // Just return and we'll try next time the scheduler fires.
					}
				}

				// In case we're using Qwen3, strip off the <think> block.
				if (summary.startsWith("<think>")) {
					summary = summary.substring(summary.indexOf("</think>") + "</think>".length());
				}
				summary = summary.strip();

				conversation.setTitle(summary);
				logger.info("Setting conversation title to " + summary);

				conversationRepository.save(conversation);

				conversationService.deleteConversation(namingConversation.getOwnerId(),
						namingConversation.getConversationId());
			}
		});
	}
}
