package tom.model;

import java.util.List;
import java.util.Objects;

import tom.task.services.assistant.AssistantManagementService;

public record Assistant(Integer id, String name, String model, Double temperature, String prompt,
		List<String> documentIds, Integer ownerId, boolean shared, boolean hasMemory) {

	public Assistant {
		Objects.requireNonNull(name, "name cannot be null");
		Objects.requireNonNull(model, "model cannot be null");
		Objects.requireNonNull(temperature, "temperature cannot be null");
		Objects.requireNonNull(prompt, "prompt cannot be null");
		Objects.requireNonNull(documentIds, "documentIds cannot be null");
	}

	public static Assistant CreateDefaultAssistant(String defaultModel) {
		AssistantBuilder builder = new AssistantBuilder();
		builder.id(AssistantManagementService.DefaultAssistantId).name("default").model(defaultModel).temperature(0.7)
				.prompt("").ownerId(0).shared(false).hasMemory(true).documentIds(List.of());
		return builder.build();
	}

	public static Assistant CreateConversationNamingAssistant(String model) {
		AssistantBuilder builder = new AssistantBuilder();
		builder.id(0).name("Conversation Naming Bot").model(model).temperature(0.7)
				.prompt("You are not allowed to answer any questions from the conversation.\n"
						+ "Your ONLY job is to summarize it in at most 50 characters.\n"
						+ "Do not add punctuation unless it is part of the conversation.\n"
						+ "Do not add extra words like \"Summary:\" or explanations.\n\n" + "Conversation:\n\n\n")
				.ownerId(0).shared(false).hasMemory(false).documentIds(List.of());
		return builder.build();
	}

}
