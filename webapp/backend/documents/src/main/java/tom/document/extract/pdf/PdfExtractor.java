package tom.document.extract.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

public class PdfExtractor {

	// If fragments-per-line ratio exceeds this, the layout is too complex for
	// heuristic extraction (multi-column, dense tables) — fall back to Tika
	private static final double FRAGMENT_RATIO_THRESHOLD = 10.0;

	public static String extract(File file) throws IOException {

		try (PDDocument doc = Loader.loadPDF(file)) {

			StructuredPdfStripper stripper = new StructuredPdfStripper();
			stripper.process(doc);

			List<PdfLine> lines = stripper.getLines();
			int fragmentCount = stripper.getFragmentCount();
			int pageCount = stripper.getPageCount();

			double ratio = lines.isEmpty() ? 0 : (double) fragmentCount / lines.size();

			if (ratio > FRAGMENT_RATIO_THRESHOLD) {
				return extractWithTika(file);
			}

			PdfTableExtractor tableExtractor = new PdfTableExtractor();
			List<PdfBlock> blocks = tableExtractor.detectBlocks(lines, pageCount);

			return PdfMarkdownRenderer.render(blocks);
		}
	}

	private static String extractWithTika(File file) throws IOException {
		try {
			BodyContentHandler handler = new BodyContentHandler(-1);
			Metadata metadata = new Metadata();
			ParseContext context = new ParseContext();
			AutoDetectParser parser = new AutoDetectParser();

			try (InputStream stream = new FileInputStream(file)) {
				parser.parse(stream, handler, metadata, context);
			}

			return cleanTikaOutput(handler.toString());

		} catch (Exception e) {
			throw new IOException("Tika extraction failed", e);
		}
	}

	/**
	 * Cleans up raw Tika output: - strips TOC dot leader lines - removes very short
	 * lines (page numbers, stray labels) - removes lines that are pure paths/URIs
	 * with no prose - collapses runs of blank lines to a single blank
	 */
	private static String cleanTikaOutput(String raw) {
		List<String> lines = Arrays.asList(raw.split("\n"));
		List<String> result = new ArrayList<>();
		boolean prevBlank = false;

		for (String line : lines) {
			String trimmed = line.trim();

			// Strip TOC dot leader lines e.g. "7.3. API definition . . . . . 24"
			if (trimmed.matches(".*(\\.\\s){4,}.*\\d+\\s*")) {
				continue;
			}

			// Strip very short lines — page numbers, stray labels, running headers
			if (trimmed.length() < 4) {
				continue;
			}

			// Strip lines that are pure URI/path tokens with no prose
			if (trimmed.matches("^[/\\w.#\\-:]+$")) {
				continue;
			}

			// Collapse multiple blank lines into one
			boolean blank = trimmed.isEmpty();
			if (blank && prevBlank) {
				continue;
			}

			result.add(line);
			prevBlank = blank;
		}

		return String.join("\n", result);
	}
}
