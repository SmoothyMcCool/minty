package tom.document.service;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.task.services.AssistantService;
import tom.task.services.DocumentService;

public class DocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DocumentProcessingTask.class);
	private final File file;
	private final int userId;
	private final int assistantId;
	private final DocumentService documentService;
	private final AssistantService assistantService;

	public DocumentProcessingTask(File file, int userId, int assistantId, DocumentService documentService,
			AssistantService assistantService) {
		this.file = file;
		this.userId = userId;
		this.assistantId = assistantId;
		this.documentService = documentService;
		this.assistantService = assistantService;
	}

	@Override
	public void run() {
		try {
			logger.info("Started processing " + file.getName());
			documentService.transformAndStore(file, userId, assistantId);
			file.delete();

			synchronized (DocumentProcessingTask.class) {
				String parent = file.getParent();
				if (parent != null) {
					File parentPath = new File(parent);
					if (parentPath.isDirectory()) {
						String[] files = parentPath.list();
						if (files.length == 0) {
							parentPath.delete();
						}
					}
				}
			}

			// Transformation might take a long time and there could be many threads, so we
			// have to update the assistant in a synchronized manner.
			markFileComplete(assistantId);
			logger.info("Processing complete for " + file.getName());
		} catch (Exception e) {
			logger.error("File processing failed: ", e);
		}
	}

	private synchronized void markFileComplete(int assistantId) {
		assistantService.fileCompleteFor(assistantId);
	}
}
