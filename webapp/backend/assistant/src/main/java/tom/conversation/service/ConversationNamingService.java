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
import tom.api.TaskPriority;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.assistant.service.management.AssistantManagementServiceInternal;
import tom.conversation.model.Conversation;
import tom.conversation.repository.ConversationRepository;
import tom.llm.service.LlmClientRegistry;

@Service
public class ConversationNamingService {

	private static final Logger logger = LogManager.getLogger(ConversationNamingService.class);

	private final ConversationRepository conversationRepository;
	private final AssistantQueryService assistantQueryService;
	private final LlmClientRegistry llmClientRegistry;
	private final ConversationServiceInternal conversationService;
	private final AssistantSpec assistantSpec;

	public ConversationNamingService(ConversationRepository conversationRepository,
			AssistantManagementServiceInternal assistantManagementService, AssistantQueryService assistantQueryService,
			ConversationServiceInternal conversationService, LlmClientRegistry llmClientRegistry) {
		this.conversationRepository = conversationRepository;
		this.assistantQueryService = assistantQueryService;
		this.llmClientRegistry = llmClientRegistry;
		this.conversationService = conversationService;
		this.assistantSpec = new AssistantSpec(AssistantManagementService.ConversationNamingAssistantId);
	}

	@Scheduled(fixedDelay = 5000)
	@Transactional
	void nameConversations() {
		List<Conversation> conversations = conversationRepository.findAllByTitle(null);

		conversations = conversations.stream().filter(conversation -> conversation
				.getAssociatedAssistantId() != AssistantManagementService.DefaultAssistantId).toList();

		conversations.forEach(conversation -> {
			List<Message> messages = llmClientRegistry.getChatMemory().get(conversation.getId().value().toString());

			if (messages.size() > 1 || (messages.size() == 1 && messages.get(0).getText().length() > 80)) {
				logger.info("Starting on conversation ID " + conversation.getId().value().toString());
				StringBuilder sb = new StringBuilder();
				messages.forEach(message -> {
					String speaker = message.getMessageType().getValue();
					String content = message.getText();
					sb.append(speaker + ": " + content);
				});

				AssistantQuery assistantQuery = new AssistantQuery();
				assistantQuery.setAssistantSpec(assistantSpec);
				tom.api.model.conversation.Conversation namingConversation = conversationService.newConversation(
						UserService.DefaultId, AssistantManagementService.ConversationNamingAssistantId);
				assistantQuery.setConversationId(namingConversation.getId());
				assistantQuery.setQuery(sb.toString());

				String summary = null;
				while (summary == null) {
					try {
						// blocks this scheduled thread, which is acceptable here
						summary = assistantQueryService.ask(UserService.DefaultId, assistantQuery, TaskPriority.Medium)
								.get();
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

				conversationService.deleteConversation(namingConversation.getOwnerId(), namingConversation.getId());
			}
		});
	}
}
