package tom.api.services.document.extract;

import java.io.File;
import java.util.List;

import tom.api.services.document.SpreadsheetFormat;

public interface DocumentExtractorService {

	String extract(File file);

	String extract(File file, SpreadsheetFormat format);

	String buildBreadcrumb(List<Section> sections, Section target);

}
