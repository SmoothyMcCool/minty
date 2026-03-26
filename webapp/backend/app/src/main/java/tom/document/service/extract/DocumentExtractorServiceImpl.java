package tom.document.service.extract;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import tom.api.services.document.SpreadsheetFormat;
import tom.api.services.document.extract.DocumentExtractorService;
import tom.api.services.document.extract.Section;
import tom.config.MintyConfiguration;
import tom.config.model.PandocConfig;
import tom.document.service.extract.pandoc.PandocConverter;
import tom.document.service.extract.pandoc.markdown.MarkdownSectionSplitter;
import tom.document.service.extract.pdf.PdfExtractor;
import tom.document.service.extract.spreadsheet.SpreadsheetExtractor;

@Service
public class DocumentExtractorServiceImpl implements DocumentExtractorService {

	private static final Logger logger = LogManager.getLogger(DocumentExtractorServiceImpl.class);

	private static final Tika tika = new Tika();
	private final PandocConfig config;
	private final PandocConverter pandoc;

	public DocumentExtractorServiceImpl(MintyConfiguration configuration) {
		this.config = configuration.getConfig().pandoc();
		this.pandoc = new PandocConverter(config.path(), config.outputFormat(), config.luaFilter(),
				config.noHighlight(), config.stripComments(), config.wrap(), config.headingLevel(), config.extraArgs());
	}

	@Override
	public String buildBreadcrumb(List<Section> sections, Section target) {
		StringBuilder crumb = new StringBuilder(target.title);
		Integer parentIndex = target.parentIndex;

		while (parentIndex != null) {
			final int idx = parentIndex;
			Section parent = sections.stream().filter(s -> s.index == idx).findFirst().orElse(null);
			if (parent == null)
				break;
			crumb.insert(0, parent.title + " > ");
			parentIndex = parent.parentIndex;
		}

		return crumb.toString();
	}

	@Override
	public List<Section> extractAndSplit(File file) {
		String markdown = extract(file);
		return MarkdownSectionSplitter.split(markdown, config.headingLevel(), config.minimumSectionSize()).stream()
				.map(section -> {
					Section s = new Section();
					s.content = section.content;
					s.index = section.index;
					s.level = section.level;
					s.parentIndex = section.parentIndex;
					s.title = section.title;
					return s;
				}).toList();
	}

	@Override
	public String extract(File file) {
		return extract(file, SpreadsheetFormat.MARKDOWN);
	}

	@Override
	public String extract(File file, SpreadsheetFormat format) {
		String mime;
		try {
			mime = tika.detect(file);

			String markdown;

			String name = file.getName().toLowerCase();
			boolean isCsv = mime.equals("text/csv") || name.endsWith(".csv");
			boolean isTsv = name.endsWith(".tsv") || name.endsWith(".tab");
			boolean isSpreadsheet = isCsv || isTsv || mime.contains("spreadsheet") || mime.contains("excel");
			boolean isPdf = mime.equals("application/pdf");

			if (panDocSupports(mime, name)) {
				markdown = pandoc.convert(file);
			} else if (isSpreadsheet) {
				markdown = SpreadsheetExtractor.extract(file,
						format == SpreadsheetFormat.MARKDOWN ? tom.document.SpreadsheetFormat.MARKDOWN
								: tom.document.SpreadsheetFormat.TSV);
			} else if (isPdf) {
				markdown = PdfExtractor.extract(file);
			} else {
				String html = parseWithTika(file);
				markdown = pandoc.convertHtmlToMarkdown(html);
			}

			return normalize(markdown);

		} catch (IOException | SAXException | TikaException | InterruptedException e) {
			logger.error("Failed to extract file " + file.getName() + ". ", e);
			return "";
		}
	}

	private boolean panDocSupports(String mime, String filename) {
		/**
		 * Checks whether the file should be processed by Pandoc based on its MIME type
		 * or file extension.
		 */
		if (config.mimeTypes().contains(mime)) {
			return true;
		}
		for (String ext : config.extensions()) {
			if (filename.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	private static String normalize(String markdown) {

		// Render to HTML then convert back to markdown if desired
		// but most pipelines simply accept normalized HTML or keep markdown.

		return markdown.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim() + "\n";
	}

	private static String parseWithTika(File file) throws IOException, SAXException, TikaException {

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