package tom.assistant.service.query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import tom.api.ConversationId;
import tom.api.UserId;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StreamResult;
import tom.api.services.assistant.StringResult;
import tom.config.ExternalProperties;
import tom.model.Assistant;
import tom.model.AssistantQuery;
import tom.ollama.service.OllamaService;
import tom.user.model.User;
import tom.user.service.UserServiceInternal;

@Service
public class AssistantQueryServiceImpl implements AssistantQueryService {

	private static final Logger logger = LogManager.getLogger(AssistantQueryServiceImpl.class);

	private final OllamaService ollamaService;
	private final OllamaApi ollamaApi;
	private final UserServiceInternal userService;
	private final AssistantManagementService assistantManagementService;
	private final ThreadPoolTaskExecutor llmExecutor;
	private final Map<ConversationId, LlmResult> results;

	public AssistantQueryServiceImpl(AssistantManagementService assistantManagementService, OllamaService ollamaService,
			OllamaApi ollamaApi, UserServiceInternal userService,
			@Qualifier("llmExecutor") ThreadPoolTaskExecutor llmExecutor, ExternalProperties properties) {
		this.ollamaService = ollamaService;
		this.ollamaApi = ollamaApi;
		this.userService = userService;
		this.assistantManagementService = assistantManagementService;
		this.llmExecutor = llmExecutor;
		this.results = new ConcurrentHashMap<>();
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
	}

	@Override
	public synchronized ConversationId askStreaming(UserId userId, AssistantQuery query) throws QueueFullException {

		try {
			LlmRequest request = new LlmRequest(userId, query, Instant.now());
			Runnable task = () -> askStreamingInternal(request);
			request.setTask(task);
			llmExecutor.execute(request);
			logger.info("Enqueued task for conversation " + query.getConversationId());

			results.put(request.getQuery().getConversationId(), LlmResult.STREAM_IN_PROGRESS);
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
		try {
			User user = userService.getUserFromId(request.getUserId()).get();

			StreamResult sr = new StreamResult();
			results.put(request.getQuery().getConversationId(), sr);

			ChatClientRequestSpec spec = prepareFromQuery(user, request.getQuery());
			if (spec == null) {
				logger.warn("askStreaming: failed to generate chat request");
				sr.addChunk("Failed to generate response.");
				sr.markComplete();
				return;
			}

			Stream<String> result = spec.stream().content().toStream();

			if (result == null) {
				sr.addChunk("Failed to generate response.");
				sr.markComplete();
				return;
			}

			result.forEach(chunk -> {
				sr.addChunk(chunk);
			});

			sr.markComplete();
			results.remove(request.getQuery().getConversationId());

		} catch (Exception e) {
			if (results.containsKey(request.getQuery().getConversationId())) {
				logger.warn("Caught exception while attempting to stream response: ", e);
				StreamResult sr = (StreamResult) results.get(request.getQuery().getConversationId());
				sr.addChunk("Failed to generate response.");
				sr.markComplete();
				return;
			}
		}

	}

	private ChatClientRequestSpec prepareFromQuery(User user, AssistantQuery query) {
		Assistant assistant = assistantManagementService.findAssistant(user.getId(), query.getAssistantId());
		if (assistant == null) {
			logger.warn(
					"Tried to build a chat session from an assistant that doesn't exist or user cannot access. User: "
							+ user.getName() + ", assistant: " + query.getAssistantId());
			return null;
		}

		return prepare(query.getQuery(), query.getConversationId(), assistant);
	}

	private ChatClientRequestSpec prepare(String query, ConversationId conversationId, Assistant assistant) {

		String model = assistant.model();

		OllamaChatModel chatModel = OllamaChatModel.builder().ollamaApi(ollamaApi).defaultOptions(OllamaOptions
				.builder().model(model).temperature(assistant.temperature()).topK(assistant.topK()).build()).build();

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
			SearchRequest searchRequest = searchRequestBuilder.topK(assistant.topK()).build();

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
				final ConversationId finalConversationId = conversationId;
				spec = spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, finalConversationId.value().toString()));
			}

			spec = spec.user(query);

		} else {
			String fullPrompt = (assistant.prompt().isBlank() ? "" : assistant.prompt() + "\n") + query;
			spec = chatClient.prompt(fullPrompt);
		}

		return spec;
	}

}
