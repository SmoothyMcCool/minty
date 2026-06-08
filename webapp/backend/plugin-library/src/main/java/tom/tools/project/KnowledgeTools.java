package tom.tools.project;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.conversation.Conversation;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.model.project.FileType;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.services.ServiceConsumer;
import tom.api.services.PluginServices;
import tom.api.tool.MintyTool;
import tom.api.tool.MintyToolResponse;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class KnowledgeTools implements MintyTool, ServiceConsumer {

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

	// =====================================================================
	// RESPONSE RECORDS
	// =====================================================================

	public enum ResultType {
		FILE, DOCUMENT
	}

	/**
	 * A single hit from knowledge_search. The type field tells the model which read
	 * tool to use next.
	 */
	public record SearchResult(
			@JsonPropertyDescription("CRITICAL: always check this field before calling any read tool. "
					+ "FILE → call knowledge_read_file with ref as the path. "
					+ "DOCUMENT → call knowledge_doc_get with ref as the title. "
					+ "Never infer type from the ref value or file extension.") ResultType type,
			@JsonPropertyDescription("For FILE: absolute path to pass to knowledge_read_file. "
					+ "For DOCUMENT: title to pass to knowledge_doc_get.") String ref,
			@JsonPropertyDescription("Brief description of the content") String summary) {
	}

	public record SectionMapEntry(
			@JsonPropertyDescription("Section index — pass to knowledge_doc_read_sections") int index,
			@JsonPropertyDescription("Section heading") String title,
			@JsonPropertyDescription("What this section covers — may be null if the document has not been summarised yet") String summary,
			@JsonPropertyDescription("Key terms found in this section — may be null if the document has not been summarised yet") List<String> keywords,
			@JsonPropertyDescription("Example questions this section can answer — may be null if the document has not been summarised yet") List<String> queries) {
	}

	public record DocumentMap(String title, List<SectionMapEntry> sections) {
	}

	public record SectionContent(@JsonPropertyDescription("Section index within the document") int index,
			@JsonPropertyDescription("Section heading") String title,
			@JsonPropertyDescription("Heading depth: 0 = top-level, higher = nested") int level,
			@JsonPropertyDescription("Index of the parent section, or null if top-level") Integer parentIndex,
			@JsonPropertyDescription("Full section content") String content) {
	}

	// =====================================================================
	// SEARCH — unified entry point
	// =====================================================================

	@Tool(name = "knowledge_search", description = """
			Search for files and documents in the project by name or keyword.

			Arguments:
			- filter: partial name, path fragment, or keyword (case-insensitive)

			Returns a ranked list of matches. Each result includes:
			- type: FILE or DOCUMENT
			- ref: for FILE, the absolute path; for DOCUMENT, the title
			- summary: a brief description of the content

			IMPORTANT: always use the type field to decide which tool to call next.
			Do not infer type from the ref value or file extension — a result with
			a .md or .json ref may be either a FILE or a DOCUMENT.

			Next steps based on type:
			- FILE     → call knowledge_read_file with the ref as the path
			- DOCUMENT → call knowledge_doc_get with the ref as the title,
			             then knowledge_doc_read_sections to read relevant sections

			Use this tool first whenever looking for content by name,
			regardless of whether it might be a file or a document.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<SearchResult>> search(
			@ToolParam(description = "Partial name, path fragment, or keyword") String filter) {
		try {
			ensureProjectSelected();

			List<SearchResult> results = new ArrayList<>();

			// File tree hits
			pluginServices.getProjectService().searchByFilter(userId, projectId, filter).stream()
					.filter(node -> node.getFileType() != null) // exclude folder nodes
					.map(node -> new SearchResult(ResultType.FILE, node.getPath(), null)).forEach(results::add);

			// Document knowledge base hits
			pluginServices.getDocumentService().listDocuments(userId, projectId).stream()
					.filter(doc -> matchesFilter(doc, filter))
					.map(doc -> new SearchResult(ResultType.DOCUMENT, doc.title(), doc.summary()))
					.forEach(results::add);

			if (results.isEmpty()) {
				return MintyToolResponse.FailureResponse("No files or documents found matching: \"" + filter + "\"");
			}

			return MintyToolResponse.SuccessResponse(results);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// =====================================================================
	// FILES
	// =====================================================================

	@Tool(name = "knowledge_read_file", description = """
			Read the full contents of a project file.

			Arguments:
			- path: absolute file path (from a FILE result in knowledge_search,
			        or from knowledge_files_tree)

			Returns the file contents, metadata, and version.

			Fails if the path does not exist or is a folder.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<NodeContent> readFile(@ToolParam(description = "Absolute file path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			NodeContent result = pluginServices.getProjectService().readNode(userId, projectId, path);
			if (result.getFileType() == null) {
				return MintyToolResponse.FailureResponse("Path refers to a folder.");
			}
			return MintyToolResponse.SuccessResponse(result);
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_files_tree", description = """
			Return the complete project file tree.

			Returns all file and folder paths with their types and versions.

			Use this to browse the full project structure when you need an
			overview rather than searching for a specific file.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> getFilesTree() {
		try {
			ensureProjectSelected();
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().describeTree(userId, projectId));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_write_file", description = """
			Create or fully replace a project file.

			Arguments:
			- path: absolute file path
			- fileType: code | markdown | json | text | diagram
			- content: complete final file contents

			Creates missing files. Fully replaces existing files — does not append or patch.
			The parent folder must already exist.

			Fails if the parent folder does not exist, the path is a folder,
			or the fileType is invalid.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> writeFile(@ToolParam(description = "Absolute file path") String path,
			@ToolParam(description = "One of: code, markdown, json, text, diagram") String fileType,
			@ToolParam(description = "Complete final file contents") String content) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			FileType parsedType;
			try {
				parsedType = FileType.valueOf(fileType);
			} catch (Exception e) {
				return MintyToolResponse
						.FailureResponse("Invalid fileType. Must be one of: code, markdown, json, text, diagram");
			}
			return MintyToolResponse.SuccessResponse(
					pluginServices.getProjectService().writeFile(userId, projectId, path, parsedType, content));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_create_folder", description = """
			Create a folder in the project file tree.

			Arguments:
			- path: absolute folder path

			The parent folder must already exist.
			Fails if the parent does not exist or the path already exists.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> createFolder(@ToolParam(description = "Absolute folder path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().createFolder(userId, projectId, path));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_delete", description = """
			Permanently delete a file or folder.

			Arguments:
			- path: absolute file or folder path

			Deleting a folder removes the full subtree. This operation is permanent.
			Fails if the path does not exist or is "/".
			""")
	@Transactional
	public MintyToolResponse<Integer> deletePath(@ToolParam(description = "Absolute file or folder path") String path) {
		try {
			ensureProjectSelected();
			PathValidator.validate(path);
			if ("/".equals(path)) {
				return MintyToolResponse.FailureResponse("Cannot delete root folder.");
			}
			return MintyToolResponse
					.SuccessResponse(pluginServices.getProjectService().deleteNode(userId, projectId, path));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_move", description = """
			Move or rename a file or folder.

			Arguments:
			- sourcePath: existing absolute path
			- targetPath: new absolute path

			Moving a folder moves its full subtree. Moved nodes receive new versions.
			Fails if the source does not exist, the target already exists,
			or a folder is moved inside itself.
			""")
	@Transactional
	public MintyToolResponse<NodeInfo> movePath(@ToolParam(description = "Existing absolute path") String sourcePath,
			@ToolParam(description = "New absolute path") String targetPath) {
		try {
			ensureProjectSelected();
			PathValidator.validate(sourcePath);
			PathValidator.validate(targetPath);
			if (targetPath.startsWith(sourcePath + "/")) {
				return MintyToolResponse.FailureResponse("Cannot move a folder inside itself.");
			}
			return MintyToolResponse.SuccessResponse(
					pluginServices.getProjectService().moveNode(userId, projectId, sourcePath, targetPath));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// =====================================================================
	// DOCUMENTS
	// =====================================================================

	@Tool(name = "knowledge_doc_get", description = """
			Retrieve a document's section list by title.

			Arguments:
			- title: document title (the ref value from a DOCUMENT result in knowledge_search)

			Returns a section list where each entry contains:
			- index: pass to knowledge_doc_read_sections to retrieve content
			- title: section heading
			- summary: what the section covers (may be null — section still has content)
			- keywords: key terms in the section (may be null)
			- queries: example questions this section can answer (may be null)

			Sections are always listed even when summary metadata is unavailable.
			A null summary does not mean the section is empty — use the section
			title and index to decide whether to fetch its content.

			Use the section list to identify which sections are relevant,
			then call knowledge_doc_read_sections with only those indices.

			Fails if no document with that title exists.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<DocumentMap> getDocument(@ToolParam(description = "Document title") String title) {
		try {
			ensureProjectSelected();

			Document document = pluginServices.getDocumentService().findByTitle(userId, projectId, title).orElse(null);

			if (document == null) {
				return MintyToolResponse.FailureResponse("No document found with title: \"" + title + "\"");
			}

			List<SectionMapEntry> sections;
			if (document.summary() != null && !document.summary().isBlank()) {
				// Rich map from summary JSON — preferred path
				sections = parseSectionMap(document.summary());
			} else {
				// No summary yet: fall back to bare section list from the document.
				// Sections are already loaded without content by listDocuments/findByTitle.
				sections = document.sections().stream()
						.map(s -> new SectionMapEntry(s.sequenceOrder(), s.title(), null, null, null)).toList();
			}

			return MintyToolResponse.SuccessResponse(new DocumentMap(document.title(), sections));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_doc_read_sections", description = """
			Read the full content of specific sections from a document.

			Arguments:
			- title: document title
			- sectionIndices: section indices to fetch (from the section map in knowledge_doc_get)

			Returns each section's heading, depth, parent index, and full content.

			Only request sections likely to contain the needed information.
			Call again with different indices if the initial results are insufficient.

			Fails if no document with that title exists or any index is out of range.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<SectionContent>> readSections(@ToolParam(description = "Document title") String title,
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

			return MintyToolResponse.SuccessResponse(sections.stream()
					.map(s -> new SectionContent(s.sequenceOrder(), s.title(), s.level(), s.parentIndex(), s.content()))
					.toList());
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// =====================================================================
	// HELPERS
	// =====================================================================

	/**
	 * Matches a document against a filter string by checking the title and the
	 * summary text, so the search covers both name and description.
	 */
	private boolean matchesFilter(Document doc, String filter) {
		String f = filter.toLowerCase();
		if (doc.title() != null && doc.title().toLowerCase().contains(f)) {
			return true;
		}
		if (doc.summary() != null && doc.summary().toLowerCase().contains(f)) {
			return true;
		}
		return false;
	}

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

	private record RawSectionMapEntry(int index, String title, RawSectionSummary summary) {
	}

	private record RawSectionSummary(Boolean insufficient, String summary, List<String> keywords,
			List<String> queries) {
	}

	// =====================================================================
	// MINTYTOOL
	// =====================================================================

	@Override
	public String name() {
		return "Knowledge Tools";
	}

	@Override
	public String description() {
		return """
				Tools for finding and reading project knowledge.

				Always start with knowledge_search to locate content by name
				or keyword — it searches both the file tree and the document
				knowledge base and tells you which read tool to use next.
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