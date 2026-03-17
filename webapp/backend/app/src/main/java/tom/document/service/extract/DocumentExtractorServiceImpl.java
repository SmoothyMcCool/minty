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
import tom.document.service.extract.pdf.PdfExtractor;
import tom.document.service.extract.spreadsheet.SpreadsheetExtractor;

@Service
public class DocumentExtractorServiceImpl implements DocumentExtractorService {

	private static final Logger logger = LogManager.getLogger(DocumentExtractorServiceImpl.class);

	private static final Tika tika = new Tika();
	private final String pandocPath;
	private final List<String> pandocMimeTypes;
	private final List<String> pandocExtensions;

	public DocumentExtractorServiceImpl(MintyConfiguration configuration) {
		this.pandocPath = configuration.getConfig().parsingConfiguration().pandocPath();
		this.pandocMimeTypes = configuration.getConfig().parsingConfiguration().pandocMimeTypes();
		this.pandocExtensions = configuration.getConfig().parsingConfiguration().pandocExtensions();
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
			boolean isPandocType = isPandocSupported(mime, name);

			if (isPandocType) {
				markdown = extractWithPandoc(file);
			} else if (isSpreadsheet) {
				markdown = SpreadsheetExtractor.extract(file, format);
			} else if (isPdf) {
				markdown = PdfExtractor.extract(file);
			} else {
				String html = parseWithTika(file);
				markdown = HtmlToMarkdown.convert(html);
			}

			return MarkdownNormalizer.normalize(markdown);

		} catch (IOException | SAXException | TikaException | InterruptedException e) {
			logger.error("Failed to extract file " + file.getName() + ". ", e);
			return "";
		}
	}

	/**
	 * Checks whether the file should be processed by Pandoc based on its MIME type
	 * or file extension.
	 */
	private boolean isPandocSupported(String mime, String fileName) {
		if (pandocMimeTypes.contains(mime)) {
			return true;
		}
		// Also match on extension for cases where MIME detection is ambiguous
		for (String ext : pandocExtensions) {
			if (fileName.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Runs pandoc on the given file, converting it to Markdown (GitHub-flavoured).
	 * Pandoc is invoked as an external process; stdout is captured as the result.
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String extractWithPandoc(File file) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(pandocPath, "--to=gfm", // GitHub-flavoured Markdown output
				"--wrap=none", // don't hard-wrap lines
				file.getAbsolutePath());
		pb.redirectErrorStream(false); // keep stderr separate so we can report it

		Process process = pb.start();

		// Read stdout (the converted Markdown)
		String output;
		try (InputStream stdout = process.getInputStream()) {
			output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
		}

		// Read stderr so we can surface any Pandoc warnings/errors
		String errors;
		try (InputStream stderr = process.getErrorStream()) {
			errors = new String(stderr.readAllBytes(), StandardCharsets.UTF_8);
		}

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			throw new RuntimeException(
					"Pandoc failed (exit " + exitCode + ") for file: " + file.getName() + "\n" + errors);
		}

		return output;
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