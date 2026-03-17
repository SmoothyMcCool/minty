package tom.document.service.extract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;

import tom.api.services.document.SpreadsheetFormat;
import tom.document.service.extract.docx.DocxExtractor;
import tom.document.service.extract.pdf.PdfExtractor;
import tom.document.service.extract.spreadsheet.SpreadsheetExtractor;

public class DocumentExtractor {

	private SpreadsheetFormat format;
	private static final Tika tika = new Tika();

	public DocumentExtractor() {
		format = SpreadsheetFormat.MARKDOWN;
	}

	public void setSpreadsheetFormat(SpreadsheetFormat format) {
		this.format = format;
	}

	public String extract(File file) throws Exception {
		String mime = tika.detect(file);

		String markdown;

		String name = file.getName().toLowerCase();
		boolean isCsv = mime.equals("text/csv") || name.endsWith(".csv");
		boolean isTsv = name.endsWith(".tsv") || name.endsWith(".tab");

		if (mime.contains("wordprocessingml")) {
			markdown = DocxExtractor.extract(file);
		} else if (isCsv || isTsv || mime.contains("spreadsheet") || mime.contains("excel")) {
			markdown = SpreadsheetExtractor.extract(file, format);
		} else if (mime.equals("application/pdf")) {
			markdown = PdfExtractor.extract(file);
		} else {
			String html = parseWithTika(file);
			markdown = HtmlToMarkdown.convert(html);
		}

		return MarkdownNormalizer.normalize(markdown);
	}

	private static String parseWithTika(File file) throws Exception {

		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		ParseContext context = new ParseContext();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ToHTMLContentHandler handler = new ToHTMLContentHandler(output, StandardCharsets.UTF_8.name());

		try (InputStream stream = TikaInputStream.get(file.toPath())) {
			parser.parse(stream, handler, metadata, context);
		}

		return output.toString(StandardCharsets.UTF_8);
	}
}