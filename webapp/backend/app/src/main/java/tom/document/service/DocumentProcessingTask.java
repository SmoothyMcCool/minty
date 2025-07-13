package tom.document.service;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.assistant.repository.Assistant;
import tom.assistant.repository.AssistantRepository;
import tom.task.services.DocumentService;

public class DocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DocumentProcessingTask.class);
	private final File file;
	private final int assistantId;
	private final AssistantRepository assistantRepository;
	private final DocumentService documentService;

	public DocumentProcessingTask(File file, int assistantId, AssistantRepository assistantRepository,
			DocumentService documentService) {
		this.file = file;
		this.assistantId = assistantId;
		this.assistantRepository = assistantRepository;
		this.documentService = documentService;
	}

	@Override
	public void run() {
		try {
			logger.info("Started processing " + file.getName());
			documentService.transformAndStore(file, assistantId);
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
			logger.error("File processing failed: " + e);
		}
	}

	private synchronized void markFileComplete(int assistantId) {
		Assistant assistant = assistantRepository.findById(assistantId).get();
		assistant.fileComplete();
		assistantRepository.save(assistant);
	}
}
