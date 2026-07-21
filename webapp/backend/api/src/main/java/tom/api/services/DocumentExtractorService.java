package tom.api.services;

import java.io.File;
import java.util.List;

import tom.api.model.document.DocumentParsingException;
import tom.api.model.document.DocumentSection;
import tom.api.model.document.SpreadsheetFormat;

public interface DocumentExtractorService {

	String extract(File file) throws DocumentParsingException;

	String extract(File file, SpreadsheetFormat format) throws DocumentParsingException;

	String buildBreadcrumb(List<DocumentSection> sections, DocumentSection target);

	List<DocumentSection> extractAndSplit(File file) throws DocumentParsingException;

}
