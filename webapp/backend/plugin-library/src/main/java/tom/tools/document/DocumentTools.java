package tom.tools.document;

import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.conversation.Conversation;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
@Deprecated
public class DocumentTools implements MintyTool, ServiceConsumer {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private PluginServices pluginServices;
	private UserId userId;
	private ConversationId conversationId;
	private ProjectId projectId;

	@Override
	public void initialize() {
		projectId = null;
		if (conversationId != null) {
			Conversation conversation = pluginServices.getConversationService().getConversation(userId, conversationId);
			projectId = conversation.getProjectId();
		}
	}

	private void ensureProjectSelected() {
		if (projectId == null) {
			throw new IllegalStateException("No project selected. User has no default project.");
		}
	}

	// ---------------------------------------------------------------------
	// RESPONSE RECORDS
	// ---------------------------------------------------------------------

	public record DocumentSummary(
			@JsonPropertyDescription("Document title — use this when calling other document tools") String title,
			@JsonPropertyDescription("Brief description of what the document contains") String summary) {
	}

	public record SectionMapEntry(
			@JsonPropertyDescription("Section index — pass this to document_get_sections to retrieve content") int index,
			@JsonPropertyDescription("Section heading") String title,
			@JsonPropertyDescription("What this section covers") String summary,
			@JsonPropertyDescription("Key terms found in this section") List<String> keywords,
			@JsonPropertyDescription("Example questions this section can answer") List<String> queries) {
	}

	public record DocumentMap(String title, @JsonProperty("sections") List<SectionMapEntry> sections) {
	}

	public record SectionContent(@JsonPropertyDescription("Section index within the document") int index,
			@JsonPropertyDescription("Section heading") String title,
			@JsonPropertyDescription("Heading depth: 0 = top-level, higher = nested") int level,
			@JsonPropertyDescription("Index of the parent section, or null if top-level") Integer parentIndex,
			@JsonPropertyDescription("Full section content") String content) {
	}

	// ---------------------------------------------------------------------
	// LIST DOCUMENTS
	// ---------------------------------------------------------------------

	@Tool(name = "document_list", description = """
			List all documents in the current project.

			Returns each document's title and a brief summary.
			Use the title with document_get_by_title to explore a document's sections.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<DocumentSummary>> listDocuments() {
		try {
			ensureProjectSelected();
			List<DocumentSummary> result = pluginServices.getDocumentService().listDocuments(userId, projectId).stream()
					.map(d -> new DocumentSummary(d.title(), d.summary())).toList();
			return MintyToolResponse.SuccessResponse(result);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// GET DOCUMENT MAP BY TITLE
	// ---------------------------------------------------------------------

	@Tool(name = "document_get_by_title", description = """
			Retrieve a document's section map by title.

			Arguments:
			- title: document title (case-insensitive, from document_list)

			Returns a section map where each entry contains:
			- index: use this when calling document_get_sections
			- title: section heading
			- summary: what the section covers
			- keywords: key terms in the section
			- queries: example questions the section can answer

			Use the section map to identify which sections are relevant
			before fetching content. Avoid fetching all sections at once.

			Fails if no document with that title exists.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<DocumentMap> getDocumentByTitle(
			@ToolParam(description = "Document title (case-insensitive)") String title) {
		try {
			ensureProjectSelected();

			Document document = pluginServices.getDocumentService().findByTitle(userId, projectId, title).orElse(null);

			if (document == null) {
				return MintyToolResponse.FailureResponse("No document found with title: \"" + title + "\"");
			}

			List<SectionMapEntry> sectionMap = parseSectionMap(document.summary());
			return MintyToolResponse.SuccessResponse(new DocumentMap(document.title(), sectionMap));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// GET SECTIONS BY INDEX
	// ---------------------------------------------------------------------

	@Tool(name = "document_get_sections", description = """
			Retrieve the full content of specific sections from a document.

			Arguments:
			- title: document title (case-insensitive)
			- sectionIndices: section indices to fetch (from the section map)

			Returns each section's heading, depth, parent index, and full content.

			Only request sections that are likely to contain the needed information.
			Fetch additional sections if the initial results are insufficient.

			Fails if:
			- no document with that title exists
			- any index is out of range
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<SectionContent>> getDocumentSections(
			@ToolParam(description = "Document title (case-insensitive)") String title,
			@ToolParam(description = "Section indices to retrieve (from the section map)") List<Integer> sectionIndices) {
		try {
			ensureProjectSelected();

			List<DocumentSection> sections = pluginServices.getDocumentService().getSectionsBySequenceOrder(userId,
					projectId, title, sectionIndices);

			if (sections == null) {
				return MintyToolResponse.FailureResponse("No document found with title: \"" + title + "\"");
			}

			List<Integer> outOfRange = sectionIndices.stream()
					.filter(i -> sections.stream().noneMatch(s -> s.sequenceOrder() == i)).toList();
			if (!outOfRange.isEmpty()) {
				return MintyToolResponse.FailureResponse("Section indices not found: " + outOfRange);
			}

			List<SectionContent> result = sections.stream()
					.map(s -> new SectionContent(s.sequenceOrder(), s.title(), s.level(), s.parentIndex(), s.content()))
					.toList();

			return MintyToolResponse.SuccessResponse(result);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// ---------------------------------------------------------------------
	// HELPERS
	// ---------------------------------------------------------------------

	/**
	 * Parses the document's summary field, which contains a JSON array of section
	 * map entries, into a list of lean {@link SectionMapEntry} records for the LLM.
	 * Returns an empty list if the summary is absent or unparseable.
	 */
	private List<SectionMapEntry> parseSectionMap(String summaryJson) {
		if (summaryJson == null || summaryJson.isBlank()) {
			return List.of();
		}
		try {
			List<RawSectionMapEntry> raw = MAPPER.readValue(summaryJson, new TypeReference<List<RawSectionMapEntry>>() {
			});
			return raw.stream().filter(e -> e.summary() != null && !Boolean.TRUE.equals(e.summary().insufficient()))
					.map(e -> new SectionMapEntry(e.index(), e.title(), e.summary().summary(),
							e.summary().keywords() != null ? e.summary().keywords() : List.of(),
							e.summary().queries() != null ? e.summary().queries() : List.of()))
					.toList();
		} catch (Exception e) {
			return List.of();
		}
	}

	// Raw deserialization records — only fields we actually need

	private record RawSectionMapEntry(int index, String title, RawSectionSummary summary) {
	}

	private record RawSectionSummary(Boolean insufficient, String summary, List<String> keywords,
			List<String> queries) {
	}

	// ---------------------------------------------------------------------

	@Override
	public String name() {
		return "Document Tools";
	}

	@Override
	public String description() {
		return """
				Read-only tools for browsing project documents
				and selectively retrieving section content.
				""";
	}

	@Override
	public void setPluginServices(PluginServices pluginServices) {
		this.pluginServices = pluginServices;
	}

	@Override
	public void setUserId(UserId userId) {
		this.userId = userId;
	}

	@Override
	public void setConversationId(ConversationId conversationId) {
		this.conversationId = conversationId;
	}

	@Override
	public boolean isPublic() {
		return false;
	}
}