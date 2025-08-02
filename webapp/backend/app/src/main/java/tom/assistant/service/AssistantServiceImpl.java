package tom.assistant.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import tom.assistant.repository.AssistantRepository;
import tom.conversation.service.ConversationServiceInternal;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.model.AssistantState;
import tom.ollama.service.MintyOllamaModel;
import tom.ollama.service.OllamaService;
import tom.task.services.DocumentService;
import tom.user.repository.User;
import tom.user.service.UserServiceInternal;

@Service
public class AssistantServiceImpl implements AssistantServiceInternal {

	private static final Logger logger = LogManager.getLogger(AssistantServiceImpl.class);

	private final AssistantRepository assistantRepository;
	private DocumentService documentService;
	private ConversationServiceInternal conversationService;
	private final OllamaService ollamaService;
	private final OllamaApi ollamaApi;
	private final UserServiceInternal userService;

	private static final int MaxResults = 5;

	public AssistantServiceImpl(AssistantRepository assistantRepository, OllamaService ollamaService,
			OllamaApi ollamaApi, UserServiceInternal userService) {
		this.assistantRepository = assistantRepository;
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.userService = userService;
	}

	@Override
	public void setDocumentService(DocumentService documentService) {
		this.documentService = documentService;
	}

	@Override
	public void setConversationService(ConversationServiceInternal conversationService) {
		this.conversationService = conversationService;
	}

	@Override
	public Assistant createAssistant(int userId, Assistant assistant) {
		tom.assistant.repository.Assistant repoAsst = new tom.assistant.repository.Assistant(assistant);
		repoAsst.setOwnerId(userId);
		repoAsst.setId(null);
		if (repoAsst.getNumFiles() > 0) {
			repoAsst.setState(AssistantState.PROCESSING_FILES);
		} else {
			repoAsst.setState(AssistantState.READY);
		}
		repoAsst.setProcessedFiles(0);

		return assistantRepository.save(repoAsst).toTaskAssistant();
	}

	@Override
	public List<Assistant> listAssistants(int userId) {
		List<tom.assistant.repository.Assistant> asstList = assistantRepository.findAllByOwnerIdOrSharedTrue(userId);

		if (asstList == null || asstList.size() == 0) {
			return new ArrayList<>();
		}

		asstList.stream().map(assistant -> {
			if (assistant.getOwnerId() == userId) {
				// Don't mark this as shared since it's owned by the current user.
				assistant.setShared(false);
			}
			return assistant;
		});

		return asstList.stream().map(asst -> asst.toTaskAssistant()).toList();
	}

	@Override
	public Assistant findAssistant(int userId, int assistantId) {
		// Special case: 0 is the default assistant
		if (assistantId == 0) {
			return Assistant.DefaultAssistant();
		}

		try {
			tom.assistant.repository.Assistant assistant = assistantRepository.findById(assistantId).get();
			if (assistant.isShared() || assistant.getOwnerId() == userId) {
				return assistant.toTaskAssistant();
			}
		} catch (Exception e) {
			logger.warn("Could not find assistant: " + assistantId);
			return Assistant.NullAssistant();
		}

		return Assistant.NullAssistant();
	}

	@Override
	public boolean deleteAssistant(int userId, int assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);

		if (assistant.Null()) {
			logger.warn("Tried to delete an assistant that doesn't exist or user cannot access. User: " + userId
					+ ", assistant: " + assistantId);
			return false;
		}

		documentService.deleteDocumentsForAssistant(userId, assistantId);
		conversationService.deleteConversationsForAssistant(userId, assistantId);
		assistantRepository.deleteById(assistantId);

		return true;

	}

	@Override
	public String ask(int userId, AssistantQuery query) {
		User user = userService.getUserFromId(userId).get();

		Optional<ChatClientRequestSpec> spec = prepare(user, query);
		if (spec.isEmpty()) {
			logger.warn("askStreaming: failed to generate chat request");
			return "";
		}

		ChatResponse chatResponse = prepare(user, query).get().call().chatResponse();

		if (chatResponse != null) {
			return chatResponse.getResult().getOutput().getText();
		}

		logger.warn("ask: Chat response was null");
		return "";
	}

	@Override
	public Stream<String> askStreaming(int userId, AssistantQuery query) {
		User user = userService.getUserFromId(userId).get();

		Optional<ChatClientRequestSpec> spec = prepare(user, query);
		if (spec.isEmpty()) {
			logger.warn("askStreaming: failed to generate chat request");
			return Stream.empty();
		}

		Stream<String> chatResponse = prepare(user, query).get().stream().content().toStream();

		if (chatResponse != null) {
			return chatResponse;
		}

		logger.warn("askStreaming: Chat response was null");
		return Stream.empty();
	}

	private Optional<ChatClientRequestSpec> prepare(User user, AssistantQuery query) {
		Assistant assistant = findAssistant(user.getId(), query.getAssistantId());
		if (assistant.Null()) {
			logger.warn(
					"Tried to build a chat session from an assistant that doesn't exist or user cannot access. User: "
							+ user.getName() + ", assistant: " + query.getAssistantId());
			return Optional.empty();
		}

		MintyOllamaModel model;
		try {
			model = MintyOllamaModel.valueOf(assistant.model());
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model() + ". Cannot continue");
			return Optional.empty();
		}

		ChatMemory chatMemory = ollamaService.getChatMemory(model);

		OllamaChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(
						OllamaOptions.builder().model(model.getName()).temperature(assistant.temperature()).build())
				.build();

		VectorStore vectorStore = ollamaService.getVectorStore(model);

		List<Advisor> advisors = new ArrayList<>();
		advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());

		boolean defaultAssistant = false;
		String conversationId = null;
		String prompt = assistant.prompt();

		if (query.getConversationId() == null || query.getConversationId().isBlank()
				|| query.getConversationId().compareTo("default") == 0) {
			defaultAssistant = true;
			conversationId = conversationService.getDefaultConversationId(user.getName());
		} else {
			conversationId = query.getConversationId();

			if (assistant.numFiles() > 0) {
				SearchRequest searchRequest = SearchRequest.builder().query(query.getQuery())
						.filterExpression(" assistantId == " + query.getAssistantId()).topK(MaxResults).build();
				Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
				advisors.add(ragAdvisor);
			}
		}

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisors).build();

		ChatClientRequestSpec spec = null;
		if (defaultAssistant || prompt.isBlank()) {
			spec = chatClient.prompt();
		} else {
			spec = chatClient.prompt(prompt);
		}

		final String finalConversationId = conversationId;
		return Optional.of(
				spec.user(query.getQuery()).advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId)));
	}

	@Override
	public void fileCompleteFor(int assistantId) {
		tom.assistant.repository.Assistant assistant = assistantRepository.findById(assistantId).get();
		assistant.fileComplete();
		assistantRepository.save(assistant);
	}

	@Override
	public String getModelForAssistant(int userId, int assistantId) {
		Assistant assistant = findAssistant(userId, assistantId);
		if (assistant.Null()) {
			logger.warn("Tried to access an assistant that does not exist or user has no permission. User " + userId
					+ ", assistant: " + assistantId);
			return "";
		}

		try {
			OllamaModel.valueOf(assistant.model()); // Just to make sure the value is valid.
			return assistant.model();
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model());
			return "";
		}
	}

}
