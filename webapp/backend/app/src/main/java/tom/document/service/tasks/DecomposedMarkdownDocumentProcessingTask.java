package tom.document.service.tasks;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import tom.api.ConversationId;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.model.conversation.Conversation;
import tom.api.model.project.FileType;
import tom.api.services.ProjectService;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.api.services.assistant.StringResult;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.api.services.document.extract.Section;
import tom.config.MintyConfiguration;
import tom.conversation.service.ConversationServiceInternal;
import tom.document.markdown.MarkdownSectionSplitter;

public class DecomposedMarkdownDocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DecomposedMarkdownDocumentProcessingTask.class);
	private final UserId userId;
	private final ProjectId projectId;
	private final File file;
	private final ProjectService projectService;
	private final ConversationServiceInternal conversationService;
	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final DocumentExtractorService documentExtractorService;
	private final String documentName;
	private final String documentFolder;
	private final MintyConfiguration config;
	List<Section> sections;

	public DecomposedMarkdownDocumentProcessingTask(UserId userId, ProjectId projectId, File file,
			ProjectService projectService, ConversationServiceInternal conversationService,
			AssistantManagementService assistantManagementService, AssistantQueryService assistantQueryService,
			DocumentExtractorService documentExtractorService, MintyConfiguration config) {
		this.userId = userId;
		this.projectId = projectId;
		this.file = file;
		this.projectService = projectService;
		this.conversationService = conversationService;
		this.assistantManagementService = assistantManagementService;
		this.assistantQueryService = assistantQueryService;
		this.documentExtractorService = documentExtractorService;
		this.config = config;

		String filename = file.getName();
		int lastDot = filename.lastIndexOf('.');
		String baseName = (lastDot == -1) ? filename : filename.substring(0, lastDot);
		documentName = slug(baseName + ".md");
		this.documentFolder = "/documents/" + documentName;
	}

	public void decompose() throws Exception {
		// Parse whole document to markdown.
		String markdown = documentExtractorService.extract(file);

		// Split into sections.
		sections = MarkdownSectionSplitter.split(markdown, config.getConfig().pandoc().headingLevel(),
				config.getConfig().pandoc().minimumSectionSize()).stream().map(section -> {
					Section s = new Section();
					s.content = section.content;
					s.index = section.index;
					s.level = section.level;
					s.parentIndex = section.parentIndex;
					s.title = section.title;
					return s;
				}).toList();
		saveSections(sections);
	}

	@Override
	public void run() {
		try {

			logger.info("Started processing " + documentName);

			String summary = writeSummary(sections);

			saveSummary(summary);

			logger.info("Decomposed markdown processing complete for " + file.getName());

		} catch (

		Exception e) {
			logger.error("Markdown processing failed: ", e);
		} finally {
			file.delete();
		}
	}

	private String writeSummary(List<Section> sections) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		ArrayNode resultArray = mapper.createArrayNode();

		for (Section s : sections) {
			String filename = sectionFilename(s);
			String breadcrumb = documentExtractorService.buildBreadcrumb(sections, s);

			String summaryStr = generateSummary(s, breadcrumb);

			ObjectNode sectionNode = mapper.createObjectNode();
			sectionNode.put("index", s.index);
			sectionNode.put("title", s.title);
			sectionNode.put("file", filename);
			sectionNode.put("path", breadcrumb);

			if (summaryStr.strip().equals("{\"insufficient\": true}")) {
				sectionNode.put("summary", "{\"insufficient\": true}");
			} else {
				try {
					JsonNode summaryJson = mapper.readTree(summaryStr);
					sectionNode.set("summary", summaryJson);
				} catch (Exception e) {
					// fallback if summary isn't valid JSON
					sectionNode.put("summary", summaryStr);
				}
			}

			resultArray.add(sectionNode);
		}

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultArray);
	}

	private String generateSummary(Section s, String breadcrumb) {
		Assistant assistant = assistantManagementService.findAssistant(UserService.DefaultId,
				AssistantManagementService.DocumentSummarizingAssistantId);

		String prompt = assistant.prompt();
		Map<String, String> values = Map.of("DOCUMENT_NAME", documentName, "SECTION_TITLE", s.title,
				"SECTION_BREADCRUMB", breadcrumb);
		StringSubstitutor sub = new StringSubstitutor(values);
		String resolvedPrompt = sub.replace(prompt);

		AssistantBuilder assistantBuilder = new AssistantBuilder(assistant);
		assistantBuilder.prompt(resolvedPrompt);

		AssistantSpec as = new AssistantSpec(null, assistantBuilder.build());
		AssistantQuery query = new AssistantQuery();
		query.setAssistantSpec(as);
		query.setQuery(s.content);
		Conversation conversation = conversationService.newConversation(UserService.DefaultId,
				AssistantManagementService.DocumentSummarizingAssistantId);
		query.setConversationId(conversation.getConversationId());

		String summary = "";

		try {
			ConversationId requestId = null;
			while (true) {
				try {
					requestId = assistantQueryService.ask(UserService.DefaultId, query);
					break;
				} catch (QueueFullException | ConversationInUseException e) {
					Thread.sleep(Duration.ofSeconds(5));
				}
			}

			while (true) {
				StringResult llmResult = (StringResult) assistantQueryService.getResultAndRemoveIfComplete(requestId);
				if (llmResult != null && llmResult.isComplete()) {
					summary = llmResult instanceof StringResult ? ((StringResult) llmResult).getValue() : "";
					break;
				}
				Thread.sleep(Duration.ofSeconds(5));
			}
			summary = summary.strip();

			conversationService.deleteConversation(conversation.getOwnerId(), conversation.getConversationId());

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("Thread was interrupted while sleeping and waiting for my turn with the LLM!");
		}

		return summary;
	}

	private void saveSummary(String summary) throws Exception {
		try {
			projectService.createFolder(userId, projectId, "/documents");
		} catch (IllegalStateException e) {
			// Nothing to do, this just means the folder already exists.
		}
		try {
			projectService.createFolder(userId, projectId, documentFolder);
		} catch (IllegalStateException e) {
			// Nothing to do, this just means the folder already exists.
		}

		projectService.writeFile(userId, projectId, documentFolder + "/" + documentName, FileType.markdown, summary);
	}

	private void saveSections(List<Section> sections) throws Exception {
		try {
			projectService.createFolder(userId, projectId, "/documents");
		} catch (IllegalStateException e) {
			// Nothing to do, this just means the folder already exists.
		}
		try {
			projectService.createFolder(userId, projectId, documentFolder);
		} catch (IllegalStateException e) {
			// Nothing to do, this just means the folder already exists.
		}
		for (Section s : sections) {
			String name = sectionFilename(s);
			projectService.writeFile(userId, projectId, documentFolder + "/" + name, FileType.markdown, s.content);
		}
	}

	/**
	 * Generates a filename that encodes both the section index and its level
	 * indentation, so files sort naturally in the project explorer. e.g.
	 * section-003-2-interface-design.md for a level-2 section at index 3
	 */
	private String sectionFilename(Section s) {
		return "section-%03d-L%d-%s.md".formatted(s.index, s.level, slug(s.title));
	}

	private static String slug(String text) {
		return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

}