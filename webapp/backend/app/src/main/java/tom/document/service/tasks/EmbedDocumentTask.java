package tom.document.service.tasks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tom.api.UserId;
import tom.api.model.document.Document;
import tom.document.service.DocumentEmbeddingService;
import tom.document.service.DocumentServiceInternal;

public class EmbedDocumentTask implements Runnable {

	private static final Logger logger = LogManager.getLogger(EmbedDocumentTask.class);

	private final UserId userId;
	private final Document doc;
	private final DocumentServiceInternal documentService;
	private final DocumentEmbeddingService documentEmbeddingService;

	public EmbedDocumentTask(UserId userId, Document doc, DocumentServiceInternal documentService,
			DocumentEmbeddingService documentEmbeddingService) {
		this.userId = userId;
		this.doc = doc;
		this.documentService = documentService;
		this.documentEmbeddingService = documentEmbeddingService;
	}

	@Override
	public void run() {
		if (doc == null) {
			logger.error("EmbedDocumentTask attempted to run with a null document.");
		}

		try {
			logger.info("Started processing " + doc.title());

			if (doc.vectorized()) {
				// No good, this document is already processed. Log a warning and delete this
				// file.
				logger.warn("Attempt to reprocess already processed document " + doc.title() + ". Deleting file.");
				return;
			}

			documentEmbeddingService.embed(doc);

			// Transformation might take a long time and there could be many threads, so we
			// have to update the assistant in a synchronized manner.
			documentService.vectorizationComplete(userId, doc.id(), true);

			logger.info("Processing complete for " + doc.title());

		} catch (Exception e) {
			logger.error("File processing failed: ", e);
			if (doc != null) {
				logger.info("Marked document as failed: " + doc.title());
				documentService.vectorizationComplete(userId, doc.id(), false);
			} else {
				logger.warn("doc was null, could not mark as failed.");
			}
		}
	}

}
