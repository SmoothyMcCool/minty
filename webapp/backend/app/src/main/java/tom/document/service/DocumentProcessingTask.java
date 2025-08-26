package tom.document.service;

import java.io.File;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.document.model.DocumentState;
import tom.document.model.MintyDoc;

public class DocumentProcessingTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(DocumentProcessingTask.class);
	private final File file;
	private final DocumentService documentService;

	public DocumentProcessingTask(File file, DocumentService documentService) {
		this.file = file;
		this.documentService = documentService;
	}

	@Override
	public void run() {
		try {
			String filename = file.getName();
			logger.info("Started processing " + filename);

			MintyDoc doc = documentService.findByDocumentId(UUID.fromString(filename));
			if (doc != null) {
				if (doc.getState() == DocumentState.READY) {
					// No good, this document is already processed. Log a warning and delete this
					// file.
					logger.warn(
							"Attempt to reprocess already processed document " + doc.getTitle() + ". Deleting file.");
					file.delete();
					return;
				}
			} else {
				logger.warn("Found file " + filename
						+ " for processing but no associated document record was found. Deleting file.");
				file.delete();
				return;
			}

			documentService.transformAndStore(file, doc);
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
			documentService.markDocumentComplete(doc);

			logger.info("Processing complete for " + file.getName());

		} catch (Exception e) {
			logger.error("File processing failed: ", e);
		}
	}

}
