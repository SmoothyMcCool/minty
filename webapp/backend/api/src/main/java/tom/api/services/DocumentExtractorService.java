package tom.api.services;

import java.io.File;
import java.util.List;

import tom.api.model.document.DocumentSection;
import tom.api.model.document.SpreadsheetFormat;

public interface DocumentExtractorService {

	String extract(File file);

	String extract(File file, SpreadsheetFormat format);

	String buildBreadcrumb(List<DocumentSection> sections, DocumentSection target);

	List<DocumentSection> extractAndSplit(File file);

}
