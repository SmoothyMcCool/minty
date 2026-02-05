package tom.assistant.service.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.ollama.api.OllamaApi;

import tom.api.ConversationId;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.LlmResult;
import tom.api.services.assistant.StringResult;
import tom.config.MintyConfiguration;
import tom.config.model.ChatModelConfig;
import tom.config.model.MintyConfig;
import tom.config.model.OllamaConfig;
import tom.ollama.service.OllamaService;
import tom.prioritythreadpool.PriorityTask;
import tom.prioritythreadpool.PriorityThreadPoolTaskExecutor;
import tom.tool.registry.ToolRegistryService;
import tom.user.service.UserServiceInternal;

/**
 * Unit tests for {@link AssistantQueryServiceImpl#getQueuePositionFor}.
 *
 * <p>
 * Only the queue‑position logic is exercised – all other collaborators are
 * mocked.
 */
class AssistantQueryServiceImplTest {

	/* --------------------------------------------------------------------- */
	/* Mocks that are injected into the service */
	/* --------------------------------------------------------------------- */
	private final AssistantManagementService assistantManagementService = mock(AssistantManagementService.class);
	private final OllamaService ollamaService = mock(OllamaService.class);
	private final OllamaApi ollamaApi = mock(OllamaApi.class);
	private final UserServiceInternal userService = mock(UserServiceInternal.class);
	private final ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);
	private MintyConfiguration mintyConfiguration = mock(MintyConfiguration.class);
	private final PriorityThreadPoolTaskExecutor llmExecutor = mock(PriorityThreadPoolTaskExecutor.class);

	private AssistantQueryServiceImpl service;

	@BeforeEach
	void setUp() {
		mintyConfiguration = mock(MintyConfiguration.class);

		MintyConfig configMock = mock(MintyConfig.class);
		OllamaConfig ollamaMock = mock(OllamaConfig.class);
		when(mintyConfiguration.getConfig()).thenReturn(configMock);
		when(configMock.ollama()).thenReturn(ollamaMock);

		ChatModelConfig modelMock = mock(ChatModelConfig.class);
		when(modelMock.name()).thenReturn("gpt-4o-mini");
		when(modelMock.defaultContext()).thenReturn(2048);
		when(modelMock.maximumContext()).thenReturn(8192);

		when(ollamaMock.chatModels()).thenReturn(List.of(modelMock));
		service = new AssistantQueryServiceImpl(assistantManagementService, ollamaService, ollamaApi, userService,
				toolRegistryService, mintyConfiguration, llmExecutor);
	}

	/* --------------------------------------------------------------------- */
	/* Helpers to inject private fields */
	/* --------------------------------------------------------------------- */
	private void setResultsMap(Map<ConversationId, LlmResult> map) throws Exception {
		var field = AssistantQueryServiceImpl.class.getDeclaredField("results");
		field.setAccessible(true);
		field.set(service, map);
	}

	private void setExecutorQueue(BlockingQueue<Runnable> queue) throws Exception {
		var threadPoolExecutor = mock(java.util.concurrent.ThreadPoolExecutor.class);
		when(llmExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
		when(threadPoolExecutor.getQueue()).thenReturn(queue);
	}

	/* --------------------------------------------------------------------- */
	/* Tests for the four possible return values of getQueuePositionFor() */
	/* --------------------------------------------------------------------- */

	@Test
	void testGetQueuePositionFor_TaskInQueue_ReturnsCorrectPosition() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		// Create two tasks: first one does not match, second one matches
		PriorityTask firstTask = mock(PriorityTask.class);
		PriorityTask secondTask = mock(PriorityTask.class);

		ConversationId otherId = new ConversationId(UUID.randomUUID());

		when(firstTask.getConversationId()).thenReturn(otherId);
		when(secondTask.getConversationId()).thenReturn(streamId);

		// Queue contains the two tasks in order
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		queue.add(firstTask);
		queue.add(secondTask);
		setExecutorQueue(queue);

		// No results in the map (task is still queued)
		setResultsMap(new ConcurrentHashMap<>());

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(2, position, "The task should be at position 2 in the queue");
	}

	@Test
	void testGetQueuePositionFor_TaskAtFrontOfQueue_ReturnsOne() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		PriorityTask task = mock(PriorityTask.class);
		when(task.getConversationId()).thenReturn(streamId);

		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		queue.add(task); // only one task, at the front
		setExecutorQueue(queue);

		setResultsMap(new ConcurrentHashMap<>());

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(1, position, "The task should be at position 1 in the queue");
	}

	@Test
	void testGetQueuePositionFor_TaskRunning_ReturnsZero() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		// No tasks in queue
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		setExecutorQueue(queue);

		// Results map contains an entry for the conversationId
		Map<ConversationId, LlmResult> results = new ConcurrentHashMap<>();
		results.put(streamId, new StringResult()); // any LlmResult will do
		setResultsMap(results);

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(0, position, "The task is running (result present), should return 0");
	}

	@Test
	void testGetQueuePositionFor_TaskNotFound_ReturnsMinusOne() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		// Empty queue
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		setExecutorQueue(queue);

		// Empty results map
		setResultsMap(new ConcurrentHashMap<>());

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(-1, position, "The task is neither queued nor running, should return -1");
	}

	@Test
	void testGetQueuePositionFor_TaskNotInQueueButRunning_ReturnsZero() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		// Queue contains a different task
		PriorityTask otherTask = mock(PriorityTask.class);
		ConversationId otherId = new ConversationId(UUID.randomUUID());
		when(otherTask.getConversationId()).thenReturn(otherId);

		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		queue.add(otherTask);
		setExecutorQueue(queue);

		// Results map contains our streamId
		Map<ConversationId, LlmResult> results = new ConcurrentHashMap<>();
		results.put(streamId, new StringResult());
		setResultsMap(results);

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(0, position, "The task is running but not in queue, should return 0");
	}

	@Test
	void testGetQueuePositionFor_TaskNotInQueueAndNotRunning_ReturnsMinusOne() throws Exception {
		// Arrange
		ConversationId streamId = new ConversationId(UUID.randomUUID());

		// Queue contains a different task
		PriorityTask otherTask = mock(PriorityTask.class);
		ConversationId otherId = new ConversationId(UUID.randomUUID());
		when(otherTask.getConversationId()).thenReturn(otherId);

		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
		queue.add(otherTask);
		setExecutorQueue(queue);

		// Empty results map
		setResultsMap(new ConcurrentHashMap<>());

		// Act
		int position = service.getQueuePositionFor(streamId);

		// Assert
		assertEquals(-1, position, "The task is neither queued nor running, should return -1");
	}
}
