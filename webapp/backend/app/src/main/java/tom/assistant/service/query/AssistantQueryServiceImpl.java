package tom.assistant.service.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.ollama.service.MintyOllamaModel;
import tom.ollama.service.OllamaService;
import tom.user.repository.User;
import tom.user.service.UserServiceInternal;

@Service
public class AssistantQueryServiceImpl implements AssistantQueryService {

	private static final Logger logger = LogManager.getLogger(AssistantQueryServiceImpl.class);

	private final OllamaService ollamaService;
	private final OllamaApi ollamaApi;
	private final UserServiceInternal userService;
	private final AssistantManagementService assistantManagementService;

	private static final int MaxResults = 2;

	public AssistantQueryServiceImpl(AssistantManagementService assistantManagementService, OllamaService ollamaService,
			OllamaApi ollamaApi, UserServiceInternal userService) {
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
	}

	@Override
	public String ask(UUID userId, AssistantQuery query) {
		User user = userService.getUserFromId(userId).get();

		Optional<ChatClientRequestSpec> spec = prepareFromQuery(user, query);
		if (spec.isEmpty()) {
			logger.warn("askStreaming: failed to generate chat request");
			return "";
		}

		ChatResponse chatResponse = spec.get().call().chatResponse();

		if (chatResponse != null) {
			return chatResponse.getResult().getOutput().getText();
		}

		logger.warn("ask: Chat response was null");
		return "";
	}

	@Override
	public Stream<String> askStreaming(UUID userId, AssistantQuery query) {
		User user = userService.getUserFromId(userId).get();

		Optional<ChatClientRequestSpec> spec = prepareFromQuery(user, query);
		if (spec.isEmpty()) {
			logger.warn("askStreaming: failed to generate chat request");
			return Stream.empty();
		}

		Stream<String> chatResponse = spec.get().stream().content().toStream();

		if (chatResponse != null) {
			return chatResponse;
		}

		logger.warn("askStreaming: Chat response was null");
		return Stream.empty();
	}

	@Override
	public String ask(Assistant assistant, String query) {

		ChatResponse chatResponse = prepare(query, null, assistant).get().call().chatResponse();
		if (chatResponse != null) {
			return chatResponse.getResult().getOutput().getText();
		}

		logger.warn("ask: Chat response was null");
		return "";
	}

	@Override
	public String ask(Assistant assistant, String query, UUID conversationId) {
		ChatResponse chatResponse = prepare(query, conversationId, assistant).get().call().chatResponse();

		if (chatResponse != null) {
			return chatResponse.getResult().getOutput().getText();
		}

		logger.warn("ask: Chat response was null");
		return "";
	}

	private Optional<ChatClientRequestSpec> prepareFromQuery(User user, AssistantQuery query) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), query.getAssistantId());
		if (assistant == null) {
			logger.warn(
					"Tried to build a chat session from an assistant that doesn't exist or user cannot access. User: "
							+ user.getName() + ", assistant: " + query.getAssistantId());
			return Optional.empty();
		}

		return prepare(query.getQuery(), query.getConversationId(), assistant);
	}

	private Optional<ChatClientRequestSpec> prepare(String query, UUID conversationId, Assistant assistant) {

		MintyOllamaModel model;
		try {
			model = MintyOllamaModel.valueOf(assistant.model());
		} catch (Exception e) {
			logger.warn("Invalid model: " + assistant.model() + ". Cannot continue");
			return Optional.empty();
		}

		OllamaChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi)
				.defaultOptions(
						OllamaOptions.builder().model(model.getName()).temperature(assistant.temperature()).build())
				.build();

		List<Advisor> advisors = new ArrayList<>();

		if (assistant.hasMemory() && conversationId != null) {
			ChatMemory chatMemory = ollamaService.getChatMemory();
			advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
		}

		if (assistant.documentIds().size() > 0) {
			VectorStore vectorStore = ollamaService.getVectorStore();

			String documentList = assistant.documentIds().stream().map(s -> "\"" + s + "\"")
					.collect(Collectors.joining(", ", "[ ", " ]"));

			Builder searchRequestBuilder = SearchRequest.builder().query(query);
			if (documentList.length() > 0) {
				searchRequestBuilder = searchRequestBuilder.filterExpression(" documentId IN " + documentList);
			}
			SearchRequest searchRequest = searchRequestBuilder.topK(MaxResults).build();

			Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
			advisors.add(ragAdvisor);
		}

		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisors).build();

		ChatClientRequestSpec spec;

		if (assistant.hasMemory() && conversationId != null) {
			spec = chatClient.prompt();
			if (!assistant.prompt().isBlank()) {
				spec = spec.system(assistant.prompt());
			}

			if (conversationId != null) {
				final UUID finalConversationId = conversationId;
				spec = spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId.toString()));
			}

			spec = spec.user(query);

		} else {
			String fullPrompt = (assistant.prompt().isBlank() ? "" : assistant.prompt() + "\n") + query;
			spec = chatClient.prompt(fullPrompt);
		}

		return Optional.of(spec);
	}

}
