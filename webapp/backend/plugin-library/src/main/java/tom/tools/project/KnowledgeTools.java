package tom.tools.project;

import java.util.Arrays;
import java.util.List;
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
import tom.api.model.document.DocumentSearchResult;
import tom.api.model.document.DocumentSection;
import tom.api.model.project.ContextLine;
import tom.api.model.project.FileType;
import tom.api.model.project.KnowledgeItemInfo;
import tom.api.model.project.KnowledgeItemType;
import tom.api.model.project.KnowledgeSearchResult;
import tom.api.model.project.NodeContent;
import tom.api.model.project.NodeInfo;
import tom.api.model.project.NodeType;
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

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record FileSearchMatch(@JsonPropertyDescription("1-based line number.") int line,
			@JsonPropertyDescription("Complete text of the matching line.") String text,
			@JsonPropertyDescription("Optional surrounding lines.") List<ContextLine> context) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record EditResult(@JsonPropertyDescription("Absolute path of the edited file.") String path,
			@JsonPropertyDescription("New file version after the edit.") int version,
			@JsonPropertyDescription("First line replaced.") int startLine,
			@JsonPropertyDescription("Last line replaced.") int endLine) {
	}

	// =====================================================================
	// SEARCH
	// =====================================================================

	@Tool(name = "knowledge_search", description = """
			Search for relevant project files and knowledge-base documents.

			Use this when you are not sure whether the information you need is
			in a project file or in a knowledge-base document.

			This is a BROAD DISCOVERY tool.

			If you know the information is inside project files, prefer
			knowledge_grep.

			If you know approximately which file or folder you need, use
			knowledge_find.

			For FILE results:
			  use knowledge_read_file(path=ref)

			For DOCUMENT results:
			  use knowledge_doc_read(title=ref)
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<SearchResult>> search(@ToolParam(description = "Text to search for") String filter) {

		try {
			ensureProjectSelected();

			if (filter == null || filter.isBlank()) {
				return MintyToolResponse.FailureResponse("Search text must not be empty.");
			}

			List<KnowledgeItemInfo> items = pluginServices.getKnowledgeService().find(userId, projectId, filter, 100);

			if (items.isEmpty()) {
				return MintyToolResponse.FailureResponse("No files or documents found matching: \"" + filter + "\"");
			}

			List<SearchResult> results = items.stream().map(this::toSearchResult).toList();

			return MintyToolResponse.SuccessResponse(results);

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_find", description = """
			Find files and folders recursively under a directory.

			Use this when you know approximately WHAT FILE or DIRECTORY you
			want, but do not know its exact path.

			This searches file and folder names/paths. It does NOT search the
			contents of files.

			Arguments:
			- path: directory subtree to search, defaults to "/"
			- name: optional filename pattern. Supports * for any characters
			  and ? for one character.
			- type: optional "File" or "Folder"
			- maxResults: maximum number of results, default 100

			Examples:

			  Find Java files:
			    knowledge_find(path="/src", name="*.java")

			  Find controllers:
			    knowledge_find(path="/src", name="*Controller.java")

			  Find folders:
			    knowledge_find(path="/src", type="Folder")

			If you know the concept you are looking for but not the filename,
			use knowledge_grep instead.

			After finding a relevant file, use knowledge_read_file to inspect it.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> findFiles(
			@ToolParam(description = "Absolute directory subtree to search. Defaults to /.", required = false) String path,
			@ToolParam(description = "Filename pattern using * and ?. Example: *Controller.java", required = false) String name,
			@ToolParam(description = "Optional node type: File or Folder.", required = false) String type,
			@ToolParam(description = "Maximum number of results. Default 100.", required = false) Integer maxResults) {

		try {
			ensureProjectSelected();

			if (path == null || path.isBlank()) {
				path = "/";
			}

			PathValidator.validate(path);

			int limit = maxResults == null ? 100 : maxResults;

			if (limit < 1 || limit > 500) {
				return MintyToolResponse.FailureResponse("maxResults must be between 1 and 500.");
			}

			NodeType nodeType = null;

			if (type != null && !type.isBlank()) {
				try {
					nodeType = NodeType.valueOf(type);
				} catch (IllegalArgumentException e) {
					return MintyToolResponse.FailureResponse("Invalid type. Must be File or Folder.");
				}
			}

			List<NodeInfo> results = pluginServices.getProjectService().find(userId, projectId, path, name, nodeType,
					limit);

			return MintyToolResponse.SuccessResponse(results);

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_grep", description = """
			Search the CONTENT of project files and knowledge-base documents.

			Use this when you know a concept, term, class, method, field,
			identifier, phrase, database object, configuration value, or other
			text that you want to locate inside the project's knowledge.

			This is the preferred tool for locating information by CONTENT.

			It searches both:
			- project files
			- knowledge-base documents

			Use the optional path argument to restrict file searches to a
			particular directory subtree.

			Examples:

			  knowledge_grep(pattern="totalTokens")
			  knowledge_grep(pattern="LlmRequest")
			  knowledge_grep(pattern="Leaderboard")
			  knowledge_grep(pattern="CREATE TABLE")
			  knowledge_grep(pattern="timeout")
			  knowledge_grep(pattern="timeout", path="/src/main/java")

			Results identify whether each match came from a FILE or DOCUMENT.

			FILE results contain:
			- file path
			- matching line numbers
			- matching text
			- optional surrounding lines

			DOCUMENT results contain:
			- document title
			- matching section
			- matching text
			- optional surrounding sections

			After finding matches:

			- If the results contain enough information to answer the question,
			  answer directly.
			- Otherwise, use knowledge_read_file for a FILE result or
			  knowledge_doc_read for a DOCUMENT result.
			- Read only the files, sections, or line ranges needed to resolve
			  the question.
			- Stop searching once sufficient evidence has been found.

			Use knowledge_find when you are searching for a file by filename or
			path rather than searching its contents.

			Use knowledge_list when you need to inspect the immediate contents
			of a directory.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<KnowledgeSearchResult>> grep(
			@ToolParam(description = "Text to search for in file and document contents.") String pattern,
			@ToolParam(description = "Optional directory subtree for file searches. Defaults to /. Documents are searched across the project.", required = false) String path,
			@ToolParam(description = "Whether matching is case-sensitive. Defaults to false.", required = false) Boolean caseSensitive,
			@ToolParam(description = "Maximum total number of matching results to return. Default 100.", required = false) Integer maxResults,
			@ToolParam(description = "Number of surrounding file lines or document sections before each match. Defaults to 0.", required = false) Integer contextBefore,
			@ToolParam(description = "Number of surrounding file lines or document sections after each match. Defaults to 0.", required = false) Integer contextAfter) {

		try {
			ensureProjectSelected();

			if (pattern == null || pattern.isBlank()) {
				return MintyToolResponse.FailureResponse("pattern must not be empty.");
			}

			boolean sensitive = caseSensitive != null && caseSensitive;

			int limit = maxResults == null ? 100 : maxResults;
			int before = contextBefore == null ? 0 : contextBefore;
			int after = contextAfter == null ? 0 : contextAfter;

			if (limit < 1 || limit > 500) {
				return MintyToolResponse.FailureResponse("maxResults must be between 1 and 500.");
			}

			if (before < 0 || before > 20) {
				return MintyToolResponse.FailureResponse("contextBefore must be between 0 and 20.");
			}

			if (after < 0 || after > 20) {
				return MintyToolResponse.FailureResponse("contextAfter must be between 0 and 20.");
			}

			List<KnowledgeSearchResult> results = pluginServices.getKnowledgeService().grep(userId, projectId, path,
					pattern, sensitive, limit, before, after);

			if (results.isEmpty()) {
				return MintyToolResponse.FailureResponse("No files or documents found containing: \"" + pattern + "\"");
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
			Read the contents of a project file.

			Arguments:
			- path: absolute file path
			- startLine: optional 1-based first line to read
			- endLine: optional 1-based last line to read

			If startLine and endLine are omitted, the entire file is returned.

			For large files, prefer reading a specific line range rather than
			the entire file.

			The response includes the file version. Use that version as
			expectedVersion when calling knowledge_edit_file.

			Typical workflow:
			  1. knowledge_grep finds a matching line
			  2. knowledge_read_file reads that line and nearby context
			  3. knowledge_edit_file modifies the relevant lines using the
			     version returned by this tool

			Examples:
			  knowledge_read_file(path="/src/main.py")
			  knowledge_read_file(path="/src/main.py", startLine=80, endLine=110)
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<NodeContent> readFile(@ToolParam(description = "Absolute file path") String path,
			@ToolParam(description = "1-based first line to read. Omit to read the entire file.", required = false) Integer startLine,
			@ToolParam(description = "1-based last line to read. Omit to read the entire file.", required = false) Integer endLine) {

		try {
			ensureProjectSelected();

			PathValidator.validate(path);

			if ((startLine == null) != (endLine == null)) {
				return MintyToolResponse
						.FailureResponse("startLine and endLine must either both be specified or both be omitted.");
			}

			NodeContent result = pluginServices.getProjectService().readNode(userId, projectId, path, startLine,
					endLine);

			if (result.getFileType() == null) {
				return MintyToolResponse.FailureResponse("Path refers to a folder.");
			}

			return MintyToolResponse.SuccessResponse(result);

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_files_tree", description = """
			Show the complete hierarchy of the project's files and folders.

			This is an OVERVIEW tool, not a general-purpose content search tool.

			Use this when you genuinely need to understand the overall structure
			of the project, for example:
			- understanding the organization of a project
			- determining the major directories or areas of the project
			- explaining the project structure to the user
			- comparing or describing project organization

			Do NOT use this merely to locate a particular piece of information.

			If you know WHAT you are looking for but do not know which file
			contains it, prefer knowledge_grep.

			If you know approximately where or what a filename is, prefer
			knowledge_find.

			If you want to inspect one directory, prefer knowledge_list.

			If you know the file path, use knowledge_read_file.

			Typical workflows:

			  Unknown concept:
			    knowledge_grep -> knowledge_read_file

			  Known filename/path:
			    knowledge_find -> knowledge_read_file

			  Specific directory:
			    knowledge_list -> knowledge_read_file

			  Overall project structure:
			    knowledge_files_tree

			The complete tree can produce a large response, so avoid using it
			when a narrower tool can answer the question.
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

	@Tool(name = "knowledge_list", description = """
			List files and folders in ONE project directory.

			Use this when you want to inspect the immediate structure of a
			specific directory.

			This does NOT search file contents and does NOT recursively return
			the entire project.

			Arguments:
			- path: directory to list, defaults to "/"
			- maxResults: maximum number of entries to return, default 100

			Examples:
			  knowledge_list()
			  knowledge_list(path="/src")
			  knowledge_list(path="/src/main")

			If you know what concept or text you are looking for, use
			knowledge_grep instead.

			If you need to find a file by name, use knowledge_find.

			If you need the complete project structure, use
			knowledge_files_tree.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<NodeInfo>> listFiles(
			@ToolParam(description = "Absolute directory path. Defaults to /.", required = false) String path,
			@ToolParam(description = "Maximum number of entries to return. Default 100.", required = false) Integer maxResults) {

		try {
			ensureProjectSelected();

			if (path == null || path.isBlank()) {
				path = "/";
			}

			PathValidator.validate(path);

			int limit = maxResults == null ? 100 : maxResults;

			if (limit < 1 || limit > 500) {
				return MintyToolResponse.FailureResponse("maxResults must be between 1 and 500.");
			}

			List<NodeInfo> nodes = pluginServices.getProjectService().listChildren(userId, projectId, path);

			if (nodes.size() > limit) {
				nodes = nodes.subList(0, limit);
			}

			return MintyToolResponse.SuccessResponse(nodes);

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	@Tool(name = "knowledge_edit_file", description = """
			Replace a specific range of lines in a project file.

			Arguments:
			- path: absolute file path
			- expectedVersion: file version returned by knowledge_read_file
			- startLine: first 1-based line to replace
			- endLine: last 1-based line to replace
			- replacement: new text that replaces those lines

			The edit is rejected if the file has changed since it was read.

			Line numbers refer to the file version that was read. Do not assume
			that line numbers remain valid after another edit.

			If you receive a version-conflict error, read the file again and
			retry the edit using the new version.

			IMPORTANT:
			Always read the relevant portion of the file before editing it.
			Use the version returned by knowledge_read_file as expectedVersion.

			When possible, use knowledge_grep first to locate the relevant lines,
			then knowledge_read_file to inspect the surrounding code before
			editing.

			Examples:

			  knowledge_edit_file(
			    path="/src/main.py",
			    expectedVersion=7,
			    startLine=42,
			    endLine=45,
			    replacement="new code here"
			  )

			The replacement completely replaces the specified line range.
			Do not include the original lines in replacement unless they should
			remain unchanged.

			Use knowledge_write_file instead when replacing an entire file or
			creating a new file.
			""")
	@Transactional
	public MintyToolResponse<EditResult> editFile(@ToolParam(description = "Absolute file path.") String path,
			@ToolParam(description = "File version returned by knowledge_read_file.") int expectedVersion,
			@ToolParam(description = "1-based first line to replace.") int startLine,
			@ToolParam(description = "1-based last line to replace.") int endLine,
			@ToolParam(description = "New text replacing the specified line range.") String replacement) {

		try {
			ensureProjectSelected();

			PathValidator.validate(path);

			if (expectedVersion < 0) {
				return MintyToolResponse.FailureResponse("expectedVersion must be >= 0.");
			}

			if (startLine < 1) {
				return MintyToolResponse.FailureResponse("startLine must be >= 1.");
			}

			if (endLine < startLine) {
				return MintyToolResponse.FailureResponse("endLine must be >= startLine.");
			}

			NodeInfo result = pluginServices.getProjectService().editFile(userId, projectId, path, expectedVersion,
					startLine, endLine, replacement);

			return MintyToolResponse
					.SuccessResponse(new EditResult(result.getPath(), result.getVersion(), startLine, endLine));

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

	// =====================================================================
	// DOCUMENTS
	// =====================================================================

	@Tool(name = "knowledge_doc_grep", description = """
			Search the CONTENT of knowledge-base documents.

			Use this when you know a concept, term, phrase, requirement,
			configuration value, database object, or other text that you want
			to locate inside knowledge-base documents.

			This searches document section content, not just document titles
			or summaries.

			Results identify the document and the matching section number.

			After finding a relevant section, use knowledge_doc_read with the
			document title and section number to retrieve the section if the
			search result does not contain enough information.

			Examples:

			  knowledge_doc_grep(pattern="deployment")
			  knowledge_doc_grep(pattern="database migration")
			  knowledge_doc_grep(pattern="timeout")

			Use knowledge_grep when searching project file contents.

			Use knowledge_search when you do not know whether the information
			is in a project file or a knowledge-base document.
			""")
	@Transactional(readOnly = true)
	public MintyToolResponse<List<DocumentSearchResult>> grepDocuments(
			@ToolParam(description = "Text to search for in document contents.") String pattern,

			@ToolParam(description = "Whether matching is case-sensitive. Defaults to false.", required = false) Boolean caseSensitive,

			@ToolParam(description = "Maximum matching sections to return. Default 100.", required = false) Integer maxResults,

			@ToolParam(description = "Number of sections before each match. Defaults to 0.", required = false) Integer contextBefore,

			@ToolParam(description = "Number of sections after each match. Defaults to 0.", required = false) Integer contextAfter) {

		try {
			ensureProjectSelected();

			if (pattern == null || pattern.isBlank()) {
				return MintyToolResponse.FailureResponse("pattern must not be empty.");
			}

			boolean sensitive = caseSensitive != null && caseSensitive;

			int limit = maxResults == null ? 100 : maxResults;

			int before = contextBefore == null ? 0 : contextBefore;

			int after = contextAfter == null ? 0 : contextAfter;

			if (limit < 1 || limit > 500) {
				return MintyToolResponse.FailureResponse("maxResults must be between 1 and 500.");
			}

			if (before < 0 || before > 20) {
				return MintyToolResponse.FailureResponse("contextBefore must be between 0 and 20.");
			}

			if (after < 0 || after > 20) {
				return MintyToolResponse.FailureResponse("contextAfter must be between 0 and 20.");
			}

			List<DocumentSearchResult> results = pluginServices.getDocumentService().grep(userId, projectId, pattern,
					sensitive, limit, before, after);

			if (results.isEmpty()) {
				return MintyToolResponse.FailureResponse("No document sections found matching: \"" + pattern + "\"");
			}

			return MintyToolResponse.SuccessResponse(results);

		} catch (Exception e) {
			return MintyToolResponse.FailureResponse(e.getMessage());
		}
	}

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
				Tools for finding, reading, and modifying engineering knowledge.

				Use the smallest number of tool calls necessary to obtain sufficient
				evidence. When the user explicitly asks you to examine all files,
				inspect all relevant files rather than stopping after understanding
				the general architecture.

				SEARCH WORKFLOW:

				1. If you know a concept, class, method, variable, table, field, or other
				   text you need to locate, use knowledge_grep.
				2. If you know a filename or path pattern, use knowledge_find.
				3. If you need to inspect one directory, use knowledge_list.
				4. Use knowledge_read_file only when the search results do not contain
				   enough information to answer the question.
				5. Stop searching once sufficient evidence has been found.

				Do not perform broad exploratory searches or retrieve the complete project
				hierarchy unless the user's question actually requires it.

				KNOWLEDGE DISCOVERY:

				Choose the narrowest tool that can answer the question.

				- knowledge_grep searches the CONTENT of project files.
				  Use this when you know what concept, term, class, configuration,
				  requirement, database object, or other information you are
				  looking for but do not know which file contains it.

				- knowledge_doc_grep searches the CONTENT of knowledge-base documents.
				  Use this when you know what text or concept you are looking for but
				  do not know which document or section contains it.

				- knowledge_doc_read reads a knowledge-base document and its sections.

				- knowledge_find searches FILE AND FOLDER NAMES/PATHS.
				  Use this when you know approximately what file or directory
				  you need.

				- knowledge_list shows the immediate contents of ONE directory.

				- knowledge_files_tree shows the COMPLETE project hierarchy.
				  Use this only when overall project structure is itself relevant.

				- knowledge_search performs broad discovery across both project
				  files and knowledge-base documents when you do not know where
				  the information is stored.

				- knowledge_read_file reads the contents of a specific project
				  file.

				- knowledge_doc_read reads a knowledge-base document and its
				  sections.

				IMPORTANT:

				Do not use knowledge_files_tree as a substitute for searching
				file contents.

				If you know WHAT you are looking for but not WHERE it is,
				prefer knowledge_grep.

				If you know approximately WHICH FILE you want,
				prefer knowledge_find.

				If you need to understand the OVERALL PROJECT STRUCTURE,
				use knowledge_files_tree.

				After locating relevant information, read the relevant file or
				document before drawing conclusions.

				FILE EDITING:

				knowledge_read_file returns a file version. When editing a file,
				pass that version to knowledge_edit_file as expectedVersion.
				This prevents edits based on stale file contents.

				Always read the relevant portion of a file before editing it.

				Use knowledge_edit_file for targeted line-based changes.

				Use knowledge_write_file when creating a file or intentionally
				replacing an entire file.

				DOCUMENT WORKFLOW:

				Use knowledge_search to locate documents when you do not know
				which document contains the information.

				Use knowledge_doc_read to inspect document structure and then
				read the relevant sections.
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

	private SearchResult toSearchResult(KnowledgeItemInfo item) {
		if (item.getType() == KnowledgeItemType.FILE) {
			return new SearchResult(ResultType.FILE, item.getPath(), item.getDescription());
		}
		return new SearchResult(ResultType.DOCUMENT, item.getName(), item.getDescription());
	}
}