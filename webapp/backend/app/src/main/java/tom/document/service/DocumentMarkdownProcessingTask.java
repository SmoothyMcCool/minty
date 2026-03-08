package tom.document.service;

import java.io.File;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.services.DocumentService;
import tom.api.services.ProjectService;

public class DocumentMarkdownProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DocumentMarkdownProcessingTask.class);
	private final UserId userId;
	private final ProjectId projectId;
	private final File file;
	private final DocumentService documentService;
	private final ProjectService projectService;

	public DocumentMarkdownProcessingTask(UserId userId, ProjectId projectId, File file,
			DocumentService documentService, ProjectService projectService) {
		this.userId = userId;
		this.projectId = projectId;
		this.file = file;
		this.documentService = documentService;
		this.projectService = projectService;
	}

	@Override
	public void run() {
		try {
			String filename = file.getName();
			logger.info("Started processing " + filename);

			String markdown = documentService.fileBytesToMarkdown(Files.readAllBytes(file.toPath()));

			projectService.writeFile(userId, projectId, "/" + filename, FileType.markdown, markdown);
			logger.info("Markdown processing complete for " + file.getName());

		} catch (Exception e) {
			logger.error("Markdown processing failed: ", e);
		} finally {
			file.delete();
		}
	}

}
