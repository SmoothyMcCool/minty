package tom.document.service.tasks;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.ProjectId;
import tom.api.UserId;
import tom.api.model.project.FileType;
import tom.api.services.ProjectService;
import tom.api.services.document.extract.DocumentExtractorService;

public class MarkdownDocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(MarkdownDocumentProcessingTask.class);
	private final UserId userId;
	private final ProjectId projectId;
	private final File file;
	private final ProjectService projectService;
	private final DocumentExtractorService documentExtractorService;

	public MarkdownDocumentProcessingTask(UserId userId, ProjectId projectId, File file, ProjectService projectService,
			DocumentExtractorService documentExtractorService) {
		this.userId = userId;
		this.projectId = projectId;
		this.file = file;
		this.projectService = projectService;
		this.documentExtractorService = documentExtractorService;
	}

	@Override
	public void run() {
		try {
			String filename = file.getName();
			int lastDot = filename.lastIndexOf('.');
			String baseName = (lastDot == -1) ? filename : filename.substring(0, lastDot);
			String newName = baseName + ".md";
			logger.info("Started processing " + newName);

			String markdown = documentExtractorService.extract(file);

			projectService.writeFile(userId, projectId, "/" + newName, FileType.markdown, markdown);
			logger.info("Markdown processing complete for " + file.getName());

		} catch (Exception e) {
			logger.error("Markdown processing failed: ", e);
		} finally {
			file.delete();
		}
	}

}
