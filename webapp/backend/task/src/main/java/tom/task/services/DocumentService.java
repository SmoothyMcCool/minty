package tom.task.services;

import java.io.File;

public interface DocumentService {

	void transformAndStore(File file, int userId, int assistantId);

	void processFile(File file);

	String constructFilename(int userId, int assistantId, String originalFilename);

	void deleteDocumentsForAssistant(int userId, int assistantId);

}
