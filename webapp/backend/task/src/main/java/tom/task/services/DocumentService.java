package tom.task.services;

import java.io.File;

public interface DocumentService {

    void transformAndStore(File file, int assistantId);

    void processFile(File file);

    void deleteDocumentsForAssistant(int assistantId);

}
