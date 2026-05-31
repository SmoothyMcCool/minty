package tom.assistant.service.query;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SearchRequest.Builder;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
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
import tom.assistant.service.agent.AgentOrchestratorService;
import tom.config.MintyConfiguration;
import tom.config.model.ChatModelConfig;
import tom.llm.service.LlmService;
import tom.meta.model.LlmRequestId;
import tom.meta.model.RequestSummary;
import tom.meta.service.AiRequestMetricsService;
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
	private final AgentOrchestratorService agentOrchestratorService;
	private final ToolRegistryService toolRegistryService;
	private final AiRequestMetricsService aiRequestMetricsService;
	private final PlatformTransactionManager transactionManager;
	private final PriorityThreadPoolTaskExecutor llmExecutor;
	private final Map<ConversationId, LlmResult> results;
	private final List<ChatModelConfig> modelConfigs;
	private final Duration streamingTimeout;
	private final ConcurrentHashMap<ConversationId, Sinks.One<Void>> activeLlmCalls;

	public AssistantQueryServiceImpl(AssistantManagementService assistantManagementService, LlmService llmService,
			UserServiceInternal userService, ToolRegistryService toolRegistryService,
			AgentOrchestratorService agentOrchestratorService, AiRequestMetricsService aiRequestMetricsService,
			PlatformTransactionManager transactionManager, MintyConfiguration properties,
			@Qualifier("llmExecutor") PriorityThreadPoolTaskExecutor llmExecutor) {
		this.llmService = llmService;
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
		this.toolRegistryService = toolRegistryService;
		this.aiRequestMetricsService = aiRequestMetricsService;
		this.transactionManager = transactionManager;
		this.llmExecutor = llmExecutor;
		this.results = new ConcurrentHashMap<>();
		this.agentOrchestratorService = agentOrchestratorService;
		this.modelConfigs = properties.getConfig().llm().modelDefinitions();
		this.streamingTimeout = properties.getConfig().llm().asyncResponseTimeout();
		this.activeLlmCalls = new ConcurrentHashMap<>();
	}

	@PostConstruct
	void init() {
		agentOrchestratorService.setAssistantQueryService(this);
	}

	@PreDestroy
	public void shutdown() {
		llmExecutor.shutdown();
	}

	@Override
	public CompletableFuture<String> ask(UserId userId, AssistantQuery query)
			throws QueueFullException, ConversationInUseException {

		LlmRequest request = new LlmRequest(userId, query, Instant.now());
		StringResult sr = new StringResult();
		sr.setState(LlmResultState.QUEUED);
		results.put(query.getConversationId(), sr);

		// We need two independent transactions in this method, so to avoid too much
		// Spring jiggery-pokery, we manage it ourselves.
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
		LlmRequestId llmRequestId = tx
				.execute(status -> aiRequestMetricsService.registerRequest(userId, query.getConversationId()));

		// Committed by here, safe to execute
		CompletableFuture<String> future = new CompletableFuture<>();

		llmExecutor.execute(() -> {
			try {
				String result = askInternal(request, llmRequestId);
				future.complete(result);
			} catch (CancellationException e) {
				future.cancel(false);
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		}, query.getConversationId(), TaskPriority.Low);

		return future;
	}

	@Override
	public String askDirect(UserId userId, AssistantQuery query) {
		TransactionTemplate tx = new TransactionTemplate(transactionManager);
		tx.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
		LlmRequestId llmRequestId = tx
				.execute(status -> aiRequestMetricsService.registerRequest(userId, query.getConversationId()));
		tx.execute(status -> aiRequestMetricsService.markDequeued(llmRequestId));

		return runSingleLlmCall(userId, llmRequestId, query);
	}

	private String askInternal(LlmRequest request, LlmRequestId llmRequestId) {
		StringResult sr = (StringResult) getResult(request.getQuery().getConversationId());
		sr.setState(LlmResultState.IN_PROGRESS);
		StringBuilder result = new StringBuilder();
		boolean cancelled = false;

		try {
			aiRequestMetricsService.markDequeued(llmRequestId);

			result.append(runSingleLlmCall(request.getUserId(), llmRequestId, request.getQuery()));
			sr.setValue(result.toString());

		} catch (CancellationException e) {
			cancelled = true;
			throw e; // let it propagate out to the llmExecutor task so we can cancel properly.
		} catch (Exception e) {
			logger.warn("askInternal failed", e);
			sr.setValue("Failed to generate response." + StackTraceUtilities.StackTrace(e));
		} finally {
			if (!cancelled) {
				sr.setState(LlmResultState.COMPLETE);
				results.put(request.getQuery().getConversationId(), sr);
			} else {
				results.remove(request.getQuery().getConversationId());
			}
		}

		return result.toString();
	}

	@Override
	public boolean cancelRequest(UserId userId, ConversationId conversationId) {
		List<RequestSummary> requests = aiRequestMetricsService.getActiveRequestsForUserAndConversation(userId,
				conversationId);

		if (requests.isEmpty()) {
			return false;
		}

		// Like highlander, there really should only be one.
		aiRequestMetricsService.markFailed(requests.get(0).llmRequestId(), "cancelled");

		boolean cancelled = llmExecutor.cancel(conversationId);
		results.remove(conversationId);

		Sinks.One<Void> sink = activeLlmCalls.get(conversationId);
		if (sink != null) {
			sink.tryEmitEmpty();
			return true;
		}
		return cancelled;
	}

	private String runSingleLlmCallStreaming(UserId userId, AssistantQuery query, LlmRequestId llmRequestId,
			StreamResult sr) {

		StringBuilder finalText = new StringBuilder();
		Set<String> docsUsed = new HashSet<>();

		String contextKey = query.getConversationId().getValue().toString();

		Sinks.One<Void> cancelSink = Sinks.one();
		this.activeLlmCalls.put(query.getConversationId(), cancelSink);

		try {

			Map<String, String> params = Map.of(ToolExecutionContext.ASSISTANT_ID,
					query.getAssistantSpec().getAssistantId() != null
							? query.getAssistantSpec().getAssistantId().value().toString()
							: "worker",
					ToolExecutionContext.REQUEST_ID, query.getConversationId().getValue().toString(),
					ToolExecutionContext.USER_ID, userId.getValue().toString());

			ToolExecutionContext.set(contextKey, params);

			User user = userService.getUserFromId(userId).orElseThrow();

			ChatClientRequestSpec spec = prepareFromQuery(user, query);

			if (spec == null) {
				logger.warn("runSingleLlmCallStreaming: failed to generate chat request");
				sr.addChunk("Failed to generate response.");
				return "";
			}

			Flux<ChatClientResponse> responses = spec.stream().chatClientResponse().timeout(streamingTimeout)
					.takeUntilOther(cancelSink.asMono());

			AtomicReference<Usage> usage = new AtomicReference<>();
			AtomicBoolean failed = new AtomicBoolean(false);
			AtomicBoolean firstToken = new AtomicBoolean(true);

			responses.publishOn(Schedulers.immediate()).doOnNext(chatClientResponse -> {

				@SuppressWarnings("unchecked")
				List<org.springframework.ai.document.Document> docs = (List<org.springframework.ai.document.Document>) chatClientResponse
						.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);

				if (docs != null && !docs.isEmpty()) {
					docs.forEach(doc -> docsUsed.add((String) doc.getMetadata().get("source")));
				}

				ChatResponse chatResponse = chatClientResponse.chatResponse();
				if (chatResponse == null || chatResponse.getResult() == null) {
					return;
				}

				if (firstToken.get()) {
					aiRequestMetricsService.recordFirstToken(llmRequestId);
					firstToken.set(false);
				}

				AssistantMessage chunk = chatResponse.getResult().getOutput();

				if (chunk != null && chunk.getText() != null) {
					String text = chunk.getText();
					sr.addChunk(text);
					finalText.append(text);
				}

				usage.set(chatResponse.getMetadata().getUsage());
			}).onErrorResume(e -> {
				logger.warn("runSingleLlmCallStreaming error", e);
				failed.set(true);
				aiRequestMetricsService.markFailed(llmRequestId, e.getMessage());
				return Flux.empty();
			}).doFinally(signalType -> {
				if (failed.get()) {
					sr.addChunk("Failed to generate response.");
				}

				if (usage.get() != null) {
					sr.addUsage(new LlmMetric(usage.get().getPromptTokens(), usage.get().getCompletionTokens()));
				}

				if (!docsUsed.isEmpty()) {
					sr.addSources(new ArrayList<>(docsUsed));
				}

				ToolExecutionContext.getAndClear(contextKey);
				if (!failed.get()) {
					// If failed is true, we marked it as failed in onErrorResume, above.
					aiRequestMetricsService.markCompleted(llmRequestId);
				}
			}).blockLast();

		} catch (Exception e) {
			logger.warn("runSingleLlmCallStreaming failed", e);
			sr.addChunk("Failed to generate response.");
		} finally {
			activeLlmCalls.remove(query.getConversationId());
		}

		return finalText.toString();
	}

	private String runSingleLlmCall(UserId userId, LlmRequestId llmRequestId, AssistantQuery query) {

		String contextKey = query.getConversationId().getValue().toString();

		try {
			Map<String, String> params = Map.of(ToolExecutionContext.ASSISTANT_ID,
					query.getAssistantSpec().getAssistantId() != null
							? query.getAssistantSpec().getAssistantId().value().toString()
							: "worker",
					ToolExecutionContext.REQUEST_ID, query.getConversationId().getValue().toString(),
					ToolExecutionContext.USER_ID, userId.getValue().toString());

			ToolExecutionContext.set(contextKey, params);

			User user = userService.getUserFromId(userId).orElseThrow();

			ChatClientRequestSpec spec = prepareFromQuery(user, query);

			if (spec == null) {
				logger.warn("runSingleLlmCall: failed to generate chat request");
				return "Failed to generate response.";
			}

			ChatResponse chatResponse = spec.call().chatResponse();

			aiRequestMetricsService.markCompleted(llmRequestId);

			if (chatResponse != null && chatResponse.getResult() != null) {
				return chatResponse.getResult().getOutput().getText();
			}

			return "LLM returned empty response.";

		} catch (Exception e) {
			if (Thread.currentThread().isInterrupted() || hasCause(e, InterruptedException.class)) {
				Thread.currentThread().interrupt();
				throw new CancellationException("LLM call interrupted");
			}
			logger.warn("runSingleLlmCall failed", e);
			return "Failed to generate response: " + e.getMessage();
		} finally {
			ToolExecutionContext.getAndClear(contextKey);
		}
	}

	private boolean hasCause(Throwable t, Class<? extends Throwable> causeClass) {
		while (t != null) {
			if (causeClass.isInstance(t))
				return true;
			t = t.getCause();
		}
		return false;
	}

	@Override
	public String askStreamingDirect(UserId userId, AssistantQuery query, StreamResult sr) {
		LlmRequestId llmRequestId = aiRequestMetricsService.registerRequest(userId, query.getConversationId());
		aiRequestMetricsService.markDequeued(llmRequestId);
		return runSingleLlmCallStreaming(userId, query, llmRequestId, sr);
	}

	@Override
	public ConversationId askStreaming(UserId userId, AssistantQuery query, StreamResult sr) throws QueueFullException {
		try {
			LlmRequest request = new LlmRequest(userId, query, Instant.now());

			LlmRequestId llmRequestId = aiRequestMetricsService.registerRequest(userId, query.getConversationId());

			llmExecutor.execute(() -> {
				aiRequestMetricsService.markDequeued(llmRequestId);

				sr.setState(LlmResultState.IN_PROGRESS);

				try {
					if (query.getAssistantSpec().getAssistantId()
							.equals(AssistantManagementService.AgenticAssistantId)) {
						agentOrchestratorService.execute(userId, query, sr);
					} else {
						runSingleLlmCallStreaming(userId, query, llmRequestId, sr);
					}
				} catch (Exception e) {
					logger.warn("Streaming Agent orchestration failed", e);
					sr.addChunk("Failed to generate response.");
				} finally {
					sr.markComplete();
					results.remove(request.getQuery().getConversationId());
				}

			}, request.getQuery().getConversationId(), TaskPriority.High);

			return request.getQuery().getConversationId();

		} catch (TaskRejectedException tre) {
			// LLM is super busy - the queue is full. Maybe next time.
			throw new QueueFullException("LLM Queue is full, try again later.");
		}
	}

	@Override
	public ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException {

		StreamResult initialStreamResult = new StreamResult(query.getQuery());
		initialStreamResult.setState(LlmResultState.QUEUED);
		results.put(query.getConversationId(), initialStreamResult);

		return askStreaming(userId, query, initialStreamResult);
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

		List<MintyTool> tools = Stream.concat(
				assistant.tools().stream()
						.map(toolName -> toolRegistryService.getTool(toolName, user.getId(), query.getConversationId()))
						.filter(Objects::nonNull),
				Stream.ofNullable(query.getProjectId() != null
						? toolRegistryService.getProjectTools(user.getId(), query.getConversationId())
						: null))
				.toList();

		ChatClient chatClient = buildChatClient(assistant, computeContextSize(assistant, query, tools), query);

		StringBuilder systemPrompt = new StringBuilder();

		ChatClientRequestSpec spec = chatClient.prompt();

		if (!tools.isEmpty()) {
			systemPrompt.append(MintyTool.ToolPrompt).append("\n\n\n");
			spec.tools(tools.toArray());
		}

		if (!assistant.prompt().isBlank()) {
			systemPrompt.append(assistant.prompt());
		}

		if (!systemPrompt.isEmpty()) {
			spec = spec.system(systemPrompt.toString());
		}

		if (assistant.hasMemory()) {
			spec = spec
					.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, query.getConversationId().value().toString()));
		}

		UserMessage message = UserMessage.builder().text(query.getQuery()).media(images).build();

		spec = spec.messages(List.of(message));

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
