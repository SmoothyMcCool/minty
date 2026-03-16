package tom.api.services.document;

import java.io.File;

public interface DocumentService {

	String fileToMarkdown(File file, SpreadsheetFormat tsv);

}
