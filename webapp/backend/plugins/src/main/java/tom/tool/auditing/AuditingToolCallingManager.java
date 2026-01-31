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
	// private final ToolAuditService auditService;
	private final String key;

	public AuditingToolCallingManager(String key, ToolCallingManager delegate) {
		// ToolAuditService auditService) {
		this.delegate = delegate;
		// this.auditService = auditService;
		this.key = key;
	}

	@Override
	public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
		// Simply delegate
		return delegate.resolveToolDefinitions(chatOptions);
	}

	@Override
	public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {

		// Pre-execution logging for any tool calls in the generations
		chatResponse.getResults().forEach(generation -> {
			AssistantMessage output = generation.getOutput();
			if (output.hasToolCalls()) {
				output.getToolCalls().forEach(toolCall -> {
				});
			}
		});

		// Delegate actual execution
		ToolExecutionResult result = delegate.executeToolCalls(prompt, chatResponse);

		// Post-execution logging
		StringBuilder sb = new StringBuilder();
		result.conversationHistory().forEach(message -> {
			if (message instanceof ToolResponseMessage toolMessage) {
				toolMessage.getResponses().forEach(toolResponse -> {
					Map<String, String> context = ToolExecutionContext.getAndClear(key);
					sb.append("user id       " + context.getOrDefault(ToolExecutionContext.USER_ID, "null"))
							.append('\n').append("tool id       " + toolResponse.id()).append('\n')
							.append("tool name     " + toolResponse.name()).append('\n')
							.append("tool response " + toolResponse.responseData()).append('\n');
				});
			}
		});

		if (sb.length() > 0) {
			logger.info("\n{}", sb.toString());
		}
		return result;
	}
}
