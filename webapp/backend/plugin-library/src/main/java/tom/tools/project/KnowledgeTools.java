package tom.tools.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonInclude;
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

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SearchResult(
			@JsonPropertyDescription("FILE -> use knowledge_read_file. DOCUMENT -> use knowledge_doc_read.") ResultType type,
			@JsonPropertyDescription("Path (FILE) or title (DOCUMENT) to use with the next tool.") String ref,
			@JsonPropertyDescription("Short description.") String summary) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SectionInfo(
			@JsonPropertyDescription("Section number. Use this in the 'sections' argument.") int index,
			@JsonPropertyDescription("Section heading.") String title,
			@JsonPropertyDescription("What this section is about.") String summary) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentMap(String title, List<SectionInfo> sections) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record SectionContent(@JsonPropertyDescription("Section number.") int index,
			@JsonPropertyDescription("Section heading.") String title,
			@JsonPropertyDescription("Full text of this section.") String content) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record DocumentContent(String title, List<SectionContent> sections) {
	}

	// =====================================================================
	// SEARCH
	// =====================================================================

	@Tool(name = "knowledge_search", description = """
			Search project files and documents by name or keyword.

			Argument: filter (text to search for - wildcards * and % are supported)

			Returns a list of results. Each result has:
			- type: FILE or DOCUMENT
			- ref: use this value with the next tool
			- summary: short description

			What to do next:
			- type=FILE -> call knowledge_read_file(path=ref)
			- type=DOCUMENT -> call knowledge_doc_read(title=ref)

			Example: knowledge_search(filter="deployment")
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<SearchResult>> search(@ToolParam(description = "Text to search for") String filter) {
		try {
			ensureProjectSelected();

			List<SearchResult> results = new ArrayList<>();

			pluginServices.getProjectService().searchByFilter(userId, projectId, filter).stream()
					.filter(node -> node.getFileType() != null)
					.map(node -> new SearchResult(ResultType.FILE, node.getPath(), null)).forEach(results::add);

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
			Read a project file's full contents.

			Argument: path (absolute file path)

			Example: knowledge_read_file(path="/src/main.py")

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
			List every file and folder in the project.

			No arguments.

			Example: knowledge_files_tree()
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
			Create a file, or replace its contents completely.

			Arguments:
			- path: absolute file path
			- fileType: one of code, markdown, json, text, diagram
			- content: the full file contents

			Example: knowledge_write_file(path="/notes/todo.md", fileType="markdown", content="# Todo\\n- task 1")

			The parent folder must already exist. This replaces the whole file, it does not append.
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
			Create a folder.

			Argument: path (absolute folder path)

			Example: knowledge_create_folder(path="/notes")

			The parent folder must already exist.
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
			Permanently delete a file or folder (and everything inside it).

			Argument: path (absolute file or folder path)

			Example: knowledge_delete(path="/notes/old.md")
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

			Example: knowledge_move(sourcePath="/notes/old.md", targetPath="/notes/new.md")
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

	@Tool(name = "knowledge_doc_read", description = """
			Read a document from the knowledge base.

			Arguments:
			- title: document title (required)
			- sections: which sections to read (optional)

			If you don't pass "sections", you get a list of section numbers
			and titles only (no content) -- use this first to see what's in
			the document.

			If you pass "sections", you get the full text of just those
			sections. You can pass section numbers as a comma-separated
			string or as a list, e.g. "0,2,3" or [0,2,3].

			Examples:
			  knowledge_doc_read(title="Setup Guide")
			  knowledge_doc_read(title="Setup Guide", sections="0,2")

			Fails if no document with that title exists.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<?> readDocument(@ToolParam(description = "Document title", required = true) String title,
			@ToolParam(description = "Section numbers to read, e.g. \"0,2,3\" or [0,2,3]. Leave empty to list sections only.", required = false) String sections) {
		try {
			ensureProjectSelected();

			Document document = pluginServices.getDocumentService().findByTitle(userId, projectId, title).orElse(null);
			if (document == null) {
				return MintyToolResponse.FailureResponse("No document found with title: \"" + title + "\"");
			}

			List<Integer> indices = parseSectionIndices(sections);

			if (indices == null || indices.isEmpty()) {
				// No sections requested -> return section list only
				List<SectionInfo> sectionInfos;
				if (document.summary() != null && !document.summary().isBlank()) {
					sectionInfos = parseSectionMap(document.summary());
				} else {
					sectionInfos = document.sections().stream()
							.map(s -> new SectionInfo(s.sequenceOrder(), s.title(), null)).toList();
				}
				return MintyToolResponse.SuccessResponse(new DocumentMap(document.title(), sectionInfos));
			}

			// Sections requested -> return full content for those sections
			List<DocumentSection> sectionData = pluginServices.getDocumentService().getSectionsBySequenceOrder(userId,
					projectId, title, indices);

			if (sectionData == null) {
				return MintyToolResponse.FailureResponse("No document found with title: \"" + title + "\"");
			}

			List<Integer> outOfRange = indices.stream()
					.filter(i -> sectionData.stream().noneMatch(s -> s.sequenceOrder() == i)).toList();
			if (!outOfRange.isEmpty()) {
				return MintyToolResponse.FailureResponse(
						"Section numbers not found: " + outOfRange + ". Call knowledge_doc_read(title=\"" + title
								+ "\") with no sections to see what's available.");
			}

			List<SectionContent> contents = sectionData.stream()
					.map(s -> new SectionContent(s.sequenceOrder(), s.title(), s.content())).toList();

			return MintyToolResponse.SuccessResponse(new DocumentContent(document.title(), contents));
		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// =====================================================================
	// HELPERS
	// =====================================================================

	private boolean matchesFilter(Document doc, String filter) {
		if (filter == null || filter.isEmpty()) {
			return true; // Or false, depending on your preference for empty searches
		}
		String regexPattern = Pattern.quote(filter);

		regexPattern = regexPattern.replace("*", "\\E.*\\Q") // Replace '*' (escaped as \* in quote) with '.*'
				.replace("?", "\\E.\\Q"); // Replace '?' (escaped as \?) with '.'

		Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);

		// Check Title
		if (doc.title() != null && pattern.matcher(doc.title()).find()) {
			return true;
		}

		// Check Summary
		if (doc.summary() != null && pattern.matcher(doc.summary()).find()) {
			return true;
		}

		return false;
	}

	/**
	 * Leniently parses a "sections" argument that might be: - null or blank -> no
	 * sections - "0,2,3" -> [0,2,3] - "[0,2,3]" -> [0,2,3] - "0" -> [0]
	 */
	private List<Integer> parseSectionIndices(String sections) {
		if (sections == null) {
			return List.of();
		}
		String cleaned = sections.trim();
		if (cleaned.isEmpty()) {
			return List.of();
		}
		// Strip surrounding brackets/quotes if the model sent JSON-array-like syntax
		cleaned = cleaned.replaceAll("^[\\[\"']+|[\\]\"']+$", "");
		if (cleaned.isEmpty()) {
			return List.of();
		}
		return Arrays.stream(cleaned.split("[,\\s]+")).filter(s -> !s.isBlank()).map(s -> {
			try {
				return Integer.parseInt(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}).filter(i -> i != null).collect(Collectors.toList());
	}

	private List<SectionInfo> parseSectionMap(String summaryJson) {
		if (summaryJson == null || summaryJson.isBlank()) {
			return List.of();
		}
		try {
			List<RawSectionMapEntry> raw = MAPPER.readValue(summaryJson, new TypeReference<List<RawSectionMapEntry>>() {
			});
			return raw.stream().filter(e -> e.summary() != null && !Boolean.TRUE.equals(e.summary().insufficient()))
					.map(e -> new SectionInfo(e.index(), e.title(), e.summary().summary())).toList();
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

				Start with knowledge_search to find files and documents by
				keyword. It tells you whether to use knowledge_read_file
				(for files) or knowledge_doc_read (for documents).
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