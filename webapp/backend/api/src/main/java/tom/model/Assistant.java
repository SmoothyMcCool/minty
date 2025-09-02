package tom.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import tom.api.services.assistant.AssistantManagementService;

public record Assistant(UUID id, String name, String model, Double temperature, String prompt, List<UUID> documentIds,
		UUID ownerId, boolean shared, boolean hasMemory) {

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
				.prompt("").ownerId(AssistantManagementService.DefaultAssistantId).shared(false).hasMemory(true)
				.documentIds(List.of());
		return builder.build();
	}

	public static Assistant CreateConversationNamingAssistant(String model) {
		AssistantBuilder builder = new AssistantBuilder();
		builder.id(AssistantManagementService.DefaultAssistantId).name("Conversation Naming Bot").model(model)
				.temperature(0.7)
				.prompt("You are not allowed to answer any questions from the conversation.\n"
						+ "Your ONLY job is to summarize it in at most 50 characters.\n"
						+ "Do not add punctuation unless it is part of the conversation.\n"
						+ "Do not add extra words like \"Summary:\" or explanations.\n\n" + "Conversation:\n\n\n")
				.ownerId(AssistantManagementService.DefaultAssistantId).shared(false).hasMemory(false)
				.documentIds(List.of());
		return builder.build();
	}

}
