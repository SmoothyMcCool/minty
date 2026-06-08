package tom.document.service.tasks;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.MintyObjectMapper;
import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.assistant.Assistant;
import tom.api.model.assistant.AssistantBuilder;
import tom.api.model.assistant.AssistantQuery;
import tom.api.model.assistant.AssistantSpec;
import tom.api.model.conversation.Conversation;
import tom.api.model.document.Document;
import tom.api.model.document.DocumentSection;
import tom.api.services.DocumentExtractorService;
import tom.api.services.UserService;
import tom.api.services.assistant.AssistantManagementService;
import tom.api.services.assistant.AssistantQueryService;
import tom.api.services.assistant.ConversationInUseException;
import tom.api.services.assistant.QueueFullException;
import tom.config.MintyConfiguration;
import tom.conversation.service.ConversationServiceInternal;
import tom.document.markdown.MarkdownSectionSplitter;
import tom.document.markdown.SectionResult;
import tom.document.markdown.SectionSummary;
import tom.document.service.DocumentServiceInternal;
import tools.jackson.databind.ObjectMapper;

public class DecomposedMarkdownDocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DecomposedMarkdownDocumentProcessingTask.class);
	private final UserId userId;
	private final String markdown;
	private final ConversationServiceInternal conversationService;
	private final AssistantManagementService assistantManagementService;
	private final AssistantQueryService assistantQueryService;
	private final DocumentExtractorService documentExtractorService;
	private final DocumentServiceInternal documentService;
	private final String documentName;
	private final MintyConfiguration config;
	private Document.Builder documentBuilder;
	private boolean summarize;
	private boolean complete;

	public DecomposedMarkdownDocumentProcessingTask(UserId userId, ProjectId projectId, String docName, String markdown,
			ConversationServiceInternal conversationService, AssistantManagementService assistantManagementService,
			AssistantQueryService assistantQueryService, DocumentExtractorService documentExtractorService,
			DocumentServiceInternal documentService, MintyConfiguration config, boolean summarize) {
		this.userId = userId;
		this.markdown = markdown;
		this.conversationService = conversationService;
		this.assistantManagementService = assistantManagementService;
		this.assistantQueryService = assistantQueryService;
		this.documentExtractorService = documentExtractorService;
		this.documentService = documentService;
		this.config = config;
		complete = false;
		this.summarize = summarize;

		documentBuilder = Document.builder().title(docName).ownerId(userId).projectId(projectId);
		documentBuilder.title(docName);
		documentName = slug(docName);
	}

	@Override
	public void run() {
		try {
			logger.info("Started processing " + documentName);

			List<DocumentSection> sections = decompose();
			documentBuilder.sections(sections);

			if (summarize) {
				String summary = writeSummary(sections);
				documentBuilder.summary(summary);
			}

			documentBuilder.created(Instant.now());
			documentBuilder.updated(Instant.now());

			Document doc = documentBuilder.build();
			documentService.addDocument(userId, doc);

			logger.info("Decomposed markdown processing complete for " + documentName);

		} catch (Exception e) {
			logger.error("Markdown processing failed: ", e);
		} finally {
			complete = true;
			documentService.taskComplete(this);
		}
	}

	public UserId getUserId() {
		return userId;
	}

	public boolean isComplete() {
		return complete;
	}

	public String getFilename() {
		return documentName;
	}

	private List<DocumentSection> decompose() throws Exception {
		List<DocumentSection> sections = MarkdownSectionSplitter.split(markdown,
				config.getConfig().pandoc().headingLevel(), config.getConfig().pandoc().minimumSectionSize());

		return sections;
	}

	private String writeSummary(List<DocumentSection> sections) throws Exception {
		ObjectMapper mapper = MintyObjectMapper.StandardJsonMapper;
		List<SectionResult> results = new ArrayList<>();

		for (DocumentSection s : sections) {
			String filename = sectionFilename(s);
			String breadcrumb = documentExtractorService.buildBreadcrumb(sections, s);
			String summaryStr = generateSummary(s, breadcrumb);

			SectionSummary summary;
			if (summaryStr.strip().equals("{\"insufficient\": true}")) {
				summary = new SectionSummary(true, null, null, null, null, null);
			} else {
				try {
					summary = mapper.readValue(summaryStr, SectionSummary.class);
				} catch (Exception e) {
					// fallback: wrap raw string in a minimal summary
					summary = new SectionSummary(false, null, summaryStr, null, null, null);
				}
			}

			results.add(new SectionResult(s.sequenceOrder(), s.title(), filename, breadcrumb, summary));
		}

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
	}

	private String generateSummary(DocumentSection s, String breadcrumb) {

		Conversation conversation = conversationService.newConversation(UserService.DefaultId,
				AssistantManagementService.DocumentSummarizingAssistantId);

		try {
			Assistant assistant = assistantManagementService.findAssistant(UserService.DefaultId,
					AssistantManagementService.DocumentSummarizingAssistantId);

			String prompt = assistant.prompt();
			Map<String, String> values = Map.of("DOCUMENT_NAME", documentName, "SECTION_TITLE", s.title(),
					"SECTION_BREADCRUMB", breadcrumb);
			StringSubstitutor sub = new StringSubstitutor(values);
			String resolvedPrompt = sub.replace(prompt);

			AssistantBuilder assistantBuilder = new AssistantBuilder(assistant);
			assistantBuilder.prompt(resolvedPrompt);

			AssistantSpec as = new AssistantSpec(assistantBuilder.build());
			AssistantQuery query = new AssistantQuery();
			query.setAssistantSpec(as);
			query.setQuery(s.content());
			query.setConversationId(conversation.getId());

			String summary = "";

			while (true) {
				try {
					summary = assistantQueryService.ask(UserService.DefaultId, query).get();
					break;
				} catch (QueueFullException | ConversationInUseException e) {
					// Thrown directly from ask() before reaching the executor
					logger.warn("LLM queue full or conversation in use, retrying in 5 seconds.");
					if (!sleepForRetry()) {
						return "";
					}
				} catch (CancellationException e) {
					logger.warn("Summary generation was cancelled.");
					return "";
				} catch (ExecutionException e) {
					if (e.getCause() instanceof QueueFullException
							|| e.getCause() instanceof ConversationInUseException) {
						logger.warn("LLM queue full or conversation in use, retrying in 5 seconds.");
						if (!sleepForRetry()) {
							return "";
						}
					} else {
						logger.warn("Summary generation failed with unexpected error.", e.getCause());
						return "";
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.warn("Thread was interrupted while waiting for LLM.");
					return "";
				}
			}

			return summary.strip();

		} finally {
			conversationService.deleteConversation(conversation.getOwnerId(), conversation.getId());
		}
	}

	private boolean sleepForRetry() {
		logger.warn("LLM queue full or conversation in use, retrying in 5 seconds.");
		try {
			Thread.sleep(Duration.ofSeconds(5));
			return true;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn("Thread was interrupted while waiting to retry.");
			return false;
		}
	}

	/**
	 * Generates a filename that encodes both the section index and its level
	 * indentation, so files sort naturally in the project explorer. e.g.
	 * section-003-2-interface-design.md for a level-2 section at index 3
	 */
	private String sectionFilename(DocumentSection s) {
		return "section-%03d-L%d-%s.md".formatted(s.sequenceOrder(), s.level(), slug(s.title()));
	}

	private static String slug(String text) {
		return text.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

}