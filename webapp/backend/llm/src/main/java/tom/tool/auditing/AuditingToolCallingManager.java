package tom.tool.auditing;

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

		// Pre-execution: log tool call parameters
		chatResponse.getResults().forEach(generation -> {
			AssistantMessage output = generation.getOutput();
			if (output.hasToolCalls()) {
				output.getToolCalls().forEach(toolCall -> {
					logger.info("Tool call dispatched - name: {}, id: {}, arguments: {}", toolCall.name(),
							toolCall.id(), toolCall.arguments());
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

					lastToolMessage.getResponses().forEach(toolResponse -> {
						Map<String, String> context = ToolExecutionContext.getAndClear(key);
						sb.append("user id       ").append(context.getOrDefault(ToolExecutionContext.USER_ID, "null"))
								.append('\n').append("tool id       ").append(toolResponse.id()).append('\n')
								.append("tool name     ").append(toolResponse.name()).append('\n')
								.append("tool response ").append(toolResponse.responseData()).append('\n');
					});

					if (sb.length() > 0) {
						logger.info("\n{}", sb.toString());
					}
				}
			}

		} catch (Exception e) {
			logger.error("Exception while calling tool: ", e);
		}

		return result;
	}
}