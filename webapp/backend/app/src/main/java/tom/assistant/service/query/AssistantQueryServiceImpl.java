package tom.assistant.service.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
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
import tom.api.services.assistant.LlmResultState;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.api.services.assistant.StringResult;
import tom.api.tool.MintyTool;
import tom.config.MintyConfiguration;
import tom.config.model.ChatModelConfig;
import tom.llm.service.LlmService;
import tom.prioritythreadpool.PriorityTask;
import tom.prioritythreadpool.PriorityThreadPoolTaskExecutor;
import tom.prioritythreadpool.TaskPriority;
import tom.tool.auditing.ToolExecutionContext;
import tom.tool.registry.ToolRegistryService;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;
import tom.util.StackTraceUtilities;

@Service
public class AssistantQueryServiceImpl implements AssistantQueryService {

	private static final Logger logger = LogManager.getLogger(AssistantQueryServiceImpl.class);

	private final LlmService llmService;
	private final UserServiceInternal userService;
	private final AssistantManagementService assistantManagementService;
	private final ToolRegistryService toolRegistryService;
	private final PriorityThreadPoolTaskExecutor llmExecutor;
	private final Map<ConversationId, LlmResult> results;
	private final List<ChatModelConfig> modelConfigs;

	public AssistantQueryServiceImpl(AssistantManagementService assistantManagementService, LlmService llmService,
			UserServiceInternal userService, ToolRegistryService toolRegistryService, MintyConfiguration properties,
			@Qualifier("llmExecutor") PriorityThreadPoolTaskExecutor llmExecutor) {
		this.llmService = llmService;
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
		this.toolRegistryService = toolRegistryService;
		this.llmExecutor = llmExecutor;
		this.results = new ConcurrentHashMap<>();
		this.modelConfigs = properties.getConfig().llm().modelDefinitions();
	}

	@PreDestroy
	public void shutdown() {
		llmExecutor.shutdown();
	}

	@Override
	public synchronized ConversationId ask(UserId userId, AssistantQuery query)
			throws QueueFullException, ConversationInUseException {

		try {
			if (results.containsKey(query.getConversationId())) {
				throw new ConversationInUseException(query.getConversationId().value().toString());
			}

			LlmRequest request = new LlmRequest(userId, query, Instant.now());
			StringResult sr = new StringResult();
			sr.setState(LlmResultState.QUEUED);
			results.put(query.getConversationId(), sr);

			llmExecutor.execute(() -> askInternal(request), query.getConversationId(), TaskPriority.Low);

			return query.getConversationId();

		} catch (TaskRejectedException tre) {
			// LLM is busy. Maybe next time.
			results.remove(query.getConversationId());
			throw new QueueFullException("LLM Queue is full, try again later.");
		}

	}

	private void askInternal(LlmRequest request) {
		StringResult sr = (StringResult) getResult(request.getQuery().getConversationId());
		sr.setState(LlmResultState.IN_PROGRESS);

		try {
			String result = "";

			User user = userService.getUserFromId(request.getUserId()).orElseThrow();

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

			sr.setValue(result);

		} catch (Exception e) {
			logger.warn("Caught exception while waiting for LLM response: ", e);
			if (results.containsKey(request.getQuery().getConversationId())) {
				sr.setValue("Failed to generate response." + StackTraceUtilities.StackTrace(e));
			}
		} finally {
			sr.setState(LlmResultState.COMPLETE);
			results.put(request.getQuery().getConversationId(), sr);
		}
	}

	@Override
	public synchronized ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException {

		try {
			LlmRequest request = new LlmRequest(userId, query, Instant.now());

			StreamResult sr = new StreamResult(query.getQuery());
			sr.setState(LlmResultState.QUEUED);
			results.put(request.getQuery().getConversationId(), sr);
			llmExecutor.execute(() -> askStreamingInternal(request), request.getQuery().getConversationId(),
					TaskPriority.High);

			return request.getQuery().getConversationId();

		} catch (TaskRejectedException tre) {
			// LLM is super busy - the queue is full. Maybe next time.
			throw new QueueFullException("LLM Queue is full, try again later.");
		}

	}

	@Override
	public LlmResult getResultAndRemoveIfComplete(ConversationId conversationId) {
		return getResultAndMaybeRemove(conversationId, true);
	}

	@Override
	public LlmResult peekLlmResult(ConversationId conversationId) {
		return getResultAndMaybeRemove(conversationId, false);
	}

	private LlmResult getResultAndMaybeRemove(ConversationId conversationId, boolean remove) {

		LlmResult result = results.get(conversationId);

		if (result == null) {
			return null;
		}

		if (result instanceof StringResult sr) {
			if (sr.isComplete() && remove) {
				results.remove(conversationId);
			}
		} else {
			StreamResult sr = (StreamResult) result;
			if (sr.isComplete() && remove) {
				results.remove(conversationId);
			}
		}
		return result;
	}

	private LlmResult getResult(ConversationId requestId) {
		return results.get(requestId);
	}

	@Override
	public int getQueuePositionFor(ConversationId streamId) {
		BlockingQueue<Runnable> queue = llmExecutor.getThreadPoolExecutor().getQueue();
		int index = 1;
		for (Runnable r : queue) {
			if (r instanceof PriorityTask pt) {
				if (pt.getConversationId().equals(streamId)) {
					return index;
				}
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
		StreamResult sr = (StreamResult) getResult(request.getQuery().getConversationId());
		sr.setState(LlmResultState.IN_PROGRESS);

		String contextKey = request.getQuery().getConversationId().getValue().toString();

		try {

			Map<String, String> params = Map.of(ToolExecutionContext.ASSISTANT_ID,
					request.getQuery().getAssistantSpec().getAssistantId().getValue().toString(),
					ToolExecutionContext.REQUEST_ID,
					request.getQuery().getConversationId() == null ? null
							: request.getQuery().getConversationId().getValue().toString(),
					ToolExecutionContext.USER_ID, request.getUserId().getValue().toString());
			ToolExecutionContext.set(contextKey, params);

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

			AtomicBoolean failed = new AtomicBoolean(false);

			responses.publishOn(Schedulers.immediate()).doOnNext(chatClientResponse -> {

				ChatResponse chatResponse = chatClientResponse.chatResponse();
				if (chatResponse == null) {
					return;
				}

				AssistantMessage chunk = chatResponse.getResult().getOutput();
				if (chunk != null && chunk.getText() != null) {
					sr.addChunk(chunk.getText());
				}

				if (chatResponse.hasToolCalls()) {
					toolCalls.addAll(chatResponse.getResult().getOutput().getToolCalls());
				}
				@SuppressWarnings("unchecked")
				List<Document> docs = (List<Document>) chatClientResponse.context()
						.get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
				if (docs != null && !docs.isEmpty()) {
					docs.forEach(doc -> {
						docsUsed.add((String) doc.getMetadata().get("source"));
					});
				}

				usage.set(chatResponse.getMetadata().getUsage());

			}).onErrorResume(e -> {
				failed.set(true);
				return Flux.empty();
			}).doFinally(signalType -> {
				try {
					if (failed.get()) {
						sr.addChunk("Failed to generate response.");
					}

					if (usage.get() != null) {
						sr.addUsage(new LlmMetric(usage.get().getPromptTokens(), usage.get().getCompletionTokens()));
					}

					if (!docsUsed.isEmpty()) {
						sr.addSources(new ArrayList<>(docsUsed));
					}

				} finally {
					sr.markComplete();
					results.remove(request.getQuery().getConversationId());
					ToolExecutionContext.getAndClear(contextKey);
				}
			}).blockLast();

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

		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(query, "query must not be null");
		Objects.requireNonNull(query.getConversationId(), "query.conversationId must not be null");
		Objects.requireNonNull(query.getAssistantSpec(), "query.assistantSpec must not be null");
		Objects.requireNonNull(query.getQuery(), "query.query must not be null");

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

		List<Media> images = new ArrayList<>();
		if (query.getContentType() != null) {
			images.add(new Media(MediaType.parseMediaType(query.getContentType()), query.getImageData()));
		}

		List<MintyTool> tools = assistant.tools().stream()
				.map(toolName -> toolRegistryService.getTool(toolName, user.getId())).filter(Objects::nonNull).toList();

		ChatClient chatClient = buildChatClient(assistant, computeContextSize(assistant, query, tools), query);

		UserMessage message = UserMessage.builder().text(query.getQuery()).media(images).build();

		ChatClientRequestSpec spec = chatClient.prompt();
		if (!assistant.prompt().isBlank()) {
			spec = spec.system(assistant.prompt());
		}

		if (assistant.hasMemory()) {
			spec = spec
					.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, query.getConversationId().value().toString()));
		}

		spec = spec.messages(List.of(message));

		if (!tools.isEmpty()) {
			spec.tools(tools.toArray());
		}

		return spec;
	}

	private ChatClient buildChatClient(Assistant assistant, int contextSize, AssistantQuery query) {
		List<Advisor> advisors = buildAdvisorList(assistant, query.getConversationId(), query.getQuery());

		return llmService.buildChatClient(assistant, query, contextSize, advisors);
	}

	private List<Advisor> buildAdvisorList(Assistant assistant, ConversationId conversationId, String query) {
		List<Advisor> advisors = new ArrayList<>();

		if (assistant.hasMemory()) {
			ChatMemory chatMemory = llmService.getChatMemory();
			advisors.add(MessageChatMemoryAdvisor.builder(chatMemory).build());
		}

		if (!assistant.documentIds().isEmpty()) {
			VectorStore vectorStore = llmService.getVectorStore();

			String documentIds = assistant.documentIds().stream().map(s -> "\"" + s.getValue().toString() + "\"")
					.collect(Collectors.joining(", ", "[ ", " ]"));

			Builder searchRequestBuilder = SearchRequest.builder().query(query);
			if (!documentIds.isEmpty()) {
				searchRequestBuilder = searchRequestBuilder.filterExpression("documentId IN " + documentIds);
			}
			SearchRequest searchRequest = searchRequestBuilder.topK(assistant.topK()).build();

			Advisor ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build();
			advisors.add(ragAdvisor);
		}

		return advisors;
	}

	private int computeContextSize(Assistant assistant, AssistantQuery query, List<MintyTool> tools) {
		int baseSize = query.getContextSize() == 0 ? assistant.contextSize() : query.getContextSize();

		return modelConfigs.stream().filter(item -> item.name().equals(assistant.model())).findFirst()
				.map(config -> Math.min(Math.max(baseSize, config.defaultContext()), config.maximumContext()))
				.orElse(baseSize);

	}
}
