package tom.model;

import java.util.List;
import java.util.Objects;

import tom.api.AssistantId;
import tom.api.DocumentId;
import tom.api.UserId;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;

public record Assistant(AssistantId id, String name, String model, Double temperature, String prompt,
		List<DocumentId> documentIds, UserId ownerId, boolean shared, boolean hasMemory) {

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
				.prompt("").ownerId(UserService.DefaultId).shared(false).hasMemory(true).documentIds(List.of());
		return builder.build();
	}

	public static Assistant CreateConversationNamingAssistant(String model) {
		AssistantBuilder builder = new AssistantBuilder();
		builder.id(AssistantManagementService.DefaultAssistantId).name("Conversation Naming Bot").model(model)
				.temperature(0.7)
				.prompt("You are not allowed to answer any questions from the conversation.\n"
						+ "Your ONLY job is to summarize it in at most 50 characters.\n"
						+ "Respond with only the summary, at most 50 characters in length.\n"
						+ "Do not add punctuation unless it is part of the conversation.\n"
						+ "Do not add extra words like \"Summary:\" or explanations.\n\n" + "Conversation:\n\n\n")
				.ownerId(UserService.DefaultId).shared(false).hasMemory(false).documentIds(List.of());
		return builder.build();
	}

	public static Assistant CreateDiagrammingAssistant(String model) {
		AssistantBuilder builder = new AssistantBuilder();
		builder.id(AssistantManagementService.DefaultAssistantId).name("Diagramming Bot").model(model).temperature(0.7)
				.prompt("You are an expert in creating and refining Mermaid.js diagrams.\n" + "\n" + "Workflow:\n"
						+ "1. When given a request, first determine if it is clear enough to produce a correct diagram.\n"
						+ "2. If it is unclear, ask concise clarifying questions to gather the missing details.\n"
						+ "3. Once enough information is provided, generate or refine the Mermaid diagram.\n"
						+ "4. If an existing diagram is provided, modify it according to the clarified instructions.\n"
						+ "\n" + "Diagram Rules:\n"
						+ "- Pick the most appropriate Mermaid diagram type (flowchart, sequence, class, state, ER, etc.).\n"
						+ "- Ensure syntax is valid and will render without changes.\n"
						+ "- Do not use parenthesis in the diagrams.\n"
						+ "- Keep labels concise and the diagram visually balanced.\n" + "\n" + "Output Format:\n"
						+ "- If clarification is needed: output only the questions, nothing else.\n"
						+ "- If ready to generate: output only the Markdown Mermaid code block.\n"
						+ "- Do not include any commentary, explanations, or HTML.\n" + "\n" + "\n" + "Input:\n" + "\n"
						+ "\n")
				.ownerId(UserService.DefaultId).shared(false).hasMemory(false).documentIds(List.of());
		return builder.build();
	}

}
