package tom.assistant.service.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Flux;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.LlmMetric;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.api.services.assistant.StringResult;
import tom.config.MintyConfiguration;
import tom.config.model.ChatModelConfig;
import tom.ollama.service.OllamaService;
import tom.tool.registry.ToolRegistryService;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;
import tom.util.StackTraceUtilities;

@Service
public class AssistantQueryServiceImpl implements AssistantQueryService {

	private static final Logger logger = LogManager.getLogger(AssistantQueryServiceImpl.class);

	private final OllamaService ollamaService;
	private final OllamaApi ollamaApi;
	private final UserServiceInternal userService;
	private final AssistantManagementService assistantManagementService;
	private final ToolRegistryService toolRegistryService;
	private final ThreadPoolTaskExecutor llmExecutor;
	private final Map<ConversationId, LlmResult> results;
	private final List<ChatModelConfig> modelConfigs;

	public AssistantQueryServiceImpl(AssistantManagementService assistantManagementService, OllamaService ollamaService,
			OllamaApi ollamaApi, UserServiceInternal userService, ToolRegistryService toolRegistryService,
			MintyConfiguration properties, @Qualifier("llmExecutor") ThreadPoolTaskExecutor llmExecutor) {
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
		this.toolRegistryService = toolRegistryService;
		this.llmExecutor = llmExecutor;
		this.results = new ConcurrentHashMap<>();
		this.modelConfigs = properties.getConfig().ollama().chatModels();
	}

	@PreDestroy
	public void shutdown() {
		llmExecutor.initiateShutdown();
	}

	@Override
	public synchronized ConversationId ask(UserId userId, AssistantQuery query)
			throws QueueFullException, ConversationInUseException {

		try {
			if (results.containsKey(query.getConversationId())) {
				throw new ConversationInUseException(query.getConversationId().value().toString());
			}

			LlmRequest request = new LlmRequest(userId, query, Instant.now());
			results.put(query.getConversationId(), LlmResult.IN_PROGRESS);

			Runnable task = () -> askInternal(request);
			request.setTask(task);
			llmExecutor.submit(task);

			return request.getQuery().getConversationId();

		} catch (TaskRejectedException tre) {
			// LLM is busy. Maybe next time.
			results.remove(query.getConversationId());
			throw new QueueFullException("LLM Queue is full, try again later.");
		}

	}

	private void askInternal(LlmRequest request) {
		try {
			String result = "";

			User user = userService.getUserFromId(request.getUserId()).get();

			ChatClientRequestSpec spec = prepareFromQuery(user, request.getQuery());

			if (spec == null) {
				logger.warn("askStreaming: failed to generate chat request");
				result = "Failed to generate chat request. Requested assistant doesn't exist or isn't shared with you.";

			} else {

				ChatResponse chatResponse = spec.call().chatResponse();

				if (chatResponse != null) {
					result = chatResponse.getResult().getOutput().getText();
				} else {
					result = "Oh no! The LLM returned a blank result. Try again later.";
				}
			}

			StringResult sr = new StringResult();
			sr.setValue(result);
			results.put(request.getQuery().getConversationId(), sr);
		} catch (Exception e) {
			logger.warn("Caught exception while waiting for LLM response: ", e);
			if (results.containsKey(request.getQuery().getConversationId())) {
				StringResult sr = new StringResult();
				sr.setValue("Failed to generate response." + StackTraceUtilities.StackTrace(e));
				results.put(request.getQuery().getConversationId(), sr);
				return;
			}
		}
	}

	@Override
	public synchronized ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException {

		try {
			LlmRequest request = new LlmRequest(userId, query, Instant.now());
			Runnable task = () -> askStreamingInternal(request);
			request.setTask(task);

			results.put(request.getQuery().getConversationId(), LlmResult.THINKING_STREAM_NOT_READY);
			llmExecutor.execute(request);
			logger.info("Enqueued task for conversation " + query.getConversationId());

			return request.getQuery().getConversationId();

		} catch (TaskRejectedException tre) {
			// LLM is super busy - the queue is full. Maybe next time.
			throw new QueueFullException("LLM Queue is full, try again later.");
		}

	}

	@Override
	public LlmResult getResultFor(ConversationId requestId) {
		if (!results.containsKey(requestId)) {
			return null;
		}

		LlmResult result = null;

		synchronized (results) {
			result = results.get(requestId);

			// We can't actually put null into the concurrent hashmap so we fake it.
			if (result == LlmResult.IN_PROGRESS) {
				return result;
			}

			if (result instanceof StringResult) {
				results.remove(requestId);
			} else {
				StreamResult sr = (StreamResult) result;
				if (sr == null) {
					logger.warn("StreamResult is null!");
					return null;
				}
				if (sr.isComplete()) {
					results.remove(requestId);
				}
			}
		}

		return result;
	}

	@Override
	public int getQueuePositionFor(ConversationId streamId) {
		BlockingQueue<Runnable> queue = llmExecutor.getThreadPoolExecutor().getQueue();
		int index = 1;
		for (Runnable r : queue) {
			LlmRequest req = (LlmRequest) r;
			if (req.getQuery().getConversationId().equals(streamId)) {
				return index;
			}
			index++;
		}

		// If we got here, the stream is not in the queue. If it is running, there is a
		// result in the results map. Return 0. If there is no result, there is nothing
		// happening. Return -1.
		if (results.containsKey(streamId)) {
			return 0;
		}
		return -1;
	}

	private void askStreamingInternal(LlmRequest request) {
		StreamResult sr = new StreamResult();

		try {
			User user = userService.getUserFromId(request.getUserId()).get();

			results.put(request.getQuery().getConversationId(), sr);

			ChatClientRequestSpec spec = prepareFromQuery(user, request.getQuery());
			if (spec == null) {
				logger.warn("askStreaming: failed to generate chat request");
				sr.addChunk("Failed to generate response.");
				sr.markComplete();
				return;
			}

			Flux<ChatClientResponse> responses = spec.stream().chatClientResponse();
			AtomicReference<Usage> usage = new AtomicReference<>();
			List<ToolCall> toolCalls = new ArrayList<>();
			Set<String> docsUsed = new HashSet<>();

			responses.doOnNext(chatClientResponse -> {

				ChatResponse chatResponse = chatClientResponse.chatResponse();
				if (chatResponse == null) {
					return;
				}

				String chunk = chatResponse.getResult().getOutput().getText();
				if (chatResponse.hasToolCalls()) {
					toolCalls.addAll(chatResponse.getResult().getOutput().getToolCalls());
				}
				@SuppressWarnings("unchecked")
				List<Document> docs = (List<Document>) chatClientResponse.context()
						.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
				if (docs != null && !docs.isEmpty()) {
					docs.forEach(doc -> {
						logger.info("found source " + doc.getMetadata().get("source"));
						docsUsed.add((String) doc.getMetadata().get("source"));
					});
				}

				if (chunk != null) {
					sr.addChunk(chunk);
				}

				usage.set(chatResponse.getMetadata().getUsage());

			}).doOnTerminate(() -> {
				sr.addUsage(new LlmMetric(usage.get().getPromptTokens(), usage.get().getCompletionTokens()));
				if (!docsUsed.isEmpty()) {
					sr.addSources(new ArrayList<>(docsUsed));
				}
				sr.markComplete();
				results.remove(request.getQuery().getConversationId());
			}).doOnError(e -> {
				logger.warn("Streaming failed for conversation {}", request.getQuery().getConversationId(), e);
			}).onErrorResume(e -> {
				if (results.containsKey(request.getQuery().getConversationId())) {
					logger.warn("Caught exception while attempting to stream response: ", e);
					sr.addChunk("Failed to generate response.");
					sr.markComplete();
				}
				return Flux.empty();
			}).subscribe();

		} catch (Exception e) {
			if (results.containsKey(request.getQuery().getConversationId())) {
				logger.warn("Caught exception while attempting to stream response: ", e);
				sr.addChunk("Failed to generate response.");
				sr.markComplete();
				return;
			}
		}

	}

	private ChatClientRequestSpec prepareFromQuery(User user, AssistantQuery query) {
		AssistantSpec assistantSpec = query.getAssistantSpec();
		Assistant assistant = assistantSpec.useId()
				? assistantManagementService.findAssistant(user.getId(), assistantSpec.getAssistantId())
				: query.getAssistantSpec().getAssistant();
		if (assistant == null) {
			logger.warn(
					"Tried to build a chat session from an assistant that doesn't exist or user cannot access. User: "
							+ user.getName() + ", assistant: " + query.getAssistantSpec().toJson());
			return null;
		}

		Media image = null;
		if (query.getContentType() != null) {
			image = new Media(MediaType.parseMediaType(query.getContentType()), query.getImageData());
		}

		int contextSize = query.getContextSize() == 0 ? assistant.contextSize() : query.getContextSize();
		return prepare(user, query.getQuery(), contextSize, query.getConversationId(), assistant, image);
	}

	private ChatClientRequestSpec prepare(User user, String query, int contextSize, ConversationId conversationId,
			Assistant assistant, Media image) {

		String model = assistant.model();

		OllamaChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi).defaultOptions(OllamaChatOptions
				.builder().model(model).temperature(assistant.temperature()).topK(assistant.topK()).build()).build();

		List<Advisor> advisors = new ArrayList<>();

		if (assistant.hasMemory() && conversationId != null) {
			ChatMemory chatMemory = ollamaService.getChatMemory();
			advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
		}

		if (assistant.documentIds().size() > 0) {
			VectorStore vectorStore = ollamaService.getVectorStore();

			String documentList = assistant.documentIds().stream().map(s -> "\"" + s.getValue().toString() + "\"")
					.collect(Collectors.joining(", ", "[ ", " ]"));

			Builder searchRequestBuilder = SearchRequest.builder().query(query);
			if (documentList.length() > 0) {
				searchRequestBuilder = searchRequestBuilder.filterExpression(" documentId IN " + documentList);
			}
			SearchRequest searchRequest = searchRequestBuilder.topK(assistant.topK()).build();

			Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
			advisors.add(ragAdvisor);
		}

		Optional<ChatModelConfig> config = modelConfigs.stream().filter(item -> item.name().equals(assistant.model()))
				.findFirst();
		if (config.isPresent()) {
			if (contextSize < config.get().defaultContext()) {
				contextSize = config.get().defaultContext();
			} else if (contextSize > config.get().maximumContext()) {
				contextSize = config.get().maximumContext();
			}
		}

		OllamaChatOptions o = OllamaChatOptions.builder().numCtx(contextSize).build();
		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(advisors).defaultOptions(o).build();

		ChatClientRequestSpec spec;

		org.springframework.ai.chat.messages.UserMessage.Builder message = UserMessage.builder().text(query);
		if (image != null) {
			message.media(List.of(image));
		}

		spec = chatClient.prompt();
		if (!assistant.prompt().isBlank()) {
			spec = spec.system(assistant.prompt());
		}

		if (assistant.hasMemory() && conversationId != null) {

			if (conversationId != null) {
				final ConversationId finalConversationId = conversationId;
				spec = spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId.value().toString()));
			}
		}

		spec = spec.messages(List.of(message.build()));

		for (String toolName : assistant.tools()) {
			Object tc = toolRegistryService.getTool(toolName, user.getId());
			if (tc != null) {
				spec.tools(tc);
			}
		}

		return spec;
	}

}
