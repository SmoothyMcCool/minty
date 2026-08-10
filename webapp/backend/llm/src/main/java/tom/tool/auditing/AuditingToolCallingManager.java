package tom.tool.auditing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

public class AuditingToolCallingManager implements ToolCallingManager {

	private final Logger logger = LogManager.getLogger(AuditingToolCallingManager.class);
	private final ToolCallingManager delegate;
	private final String key;

	public AuditingToolCallingManager(String key, ToolCallingManager delegate) {
		this.delegate = delegate;
		this.key = key;
	}

	@Override
	public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
		return delegate.resolveToolDefinitions(chatOptions);
	}

	@Override
	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {

		// Pre-execution: log tool call parameters and remember name/arguments by id,
		// so we can pair each result with its call once execution completes.
		Map<String, String> pendingCallsById = new HashMap<>();

		chatResponse.getResults().forEach(generation -> {
			AssistantMessage output = generation.getOutput();
			if (output.hasToolCalls()) {
				output.getToolCalls().forEach(toolCall -> {
					logger.info("Tool call dispatched - name: {}, id: {}, arguments: {}", toolCall.name(),
							toolCall.id(), toolCall.arguments());
					pendingCallsById.put(toolCall.id(), toolCall.name() + "(" + toolCall.arguments() + ")");
				});
			}
		});

		ToolExecutionResult result = null;
		try {
			result = delegate.executeToolCalls(prompt, chatResponse);

			if (result != null) {
				List<ToolResponseMessage> toolMessages = result.conversationHistory().stream()
						.filter(m -> m instanceof ToolResponseMessage).map(m -> (ToolResponseMessage) m).toList();

				if (!toolMessages.isEmpty()) {
					ToolResponseMessage lastToolMessage = toolMessages.getLast();
					StringBuilder sb = new StringBuilder();
					List<String> completedEntries = new ArrayList<>();

					lastToolMessage.getResponses().forEach(toolResponse -> {
						Map<String, Object> context = ToolExecutionContext.get(key);
						sb.append("user id      : ").append(context.getOrDefault(ToolExecutionContext.USER_ID, "null"))
								.append('\n').append("tool id      : ").append(toolResponse.id()).append('\n')
								.append("tool name    : ").append(toolResponse.name()).append('\n')
								.append("tool response: ")
								.append((toolResponse.responseData().length() / 3.5) + " tokens (approximate)")
								.append('\n');

						String callDescription = pendingCallsById.get(toolResponse.id());
						if (callDescription == null) {
							// Fall back if we somehow don't have a matching dispatched call recorded.
							callDescription = toolResponse.name() + "(?)";
							logger.warn("No matching dispatched call found for tool response id {}",
									toolResponse.id());
						}

						completedEntries.add(callDescription + " \n\n " + toolResponse.responseData());
					});

					if (sb.length() > 0) {
						logger.info("\n{}", sb.toString());
					}

					if (!completedEntries.isEmpty()) {
						appendAccumulatedToolCalls(completedEntries);
					}
				}
			}

		} catch (Exception e) {
			logger.error("Exception while calling tool: ", e);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private void appendAccumulatedToolCalls(List<String> newEntries) {
		Map<String, Object> currentParams = ToolExecutionContext.get(key);
		if (currentParams != null) {
			// Create a modifiable map copy if the input map was immutable (e.g., Map.of)
			Map<String, Object> modifiableParams = new HashMap<>(currentParams);

			// Fetch existing stored tools to avoid overwriting multi-turn agent execution
			// records
			Object existing = modifiableParams.get(ToolExecutionContext.ACCUMULATED_TOOL_CALLS);
			List<String> accumulated = (existing instanceof List) ? new ArrayList<>((List<String>) existing)
					: new ArrayList<>();

			accumulated.addAll(newEntries);

			modifiableParams.put(ToolExecutionContext.ACCUMULATED_TOOL_CALLS, accumulated);
			ToolExecutionContext.set(key, modifiableParams);
		}
	}
}