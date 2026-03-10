package tom.document.service.markdown;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.ToHTMLContentHandler;

public class DocumentParser {

	public static String parse(File file) throws Exception {

		AutoDetectParser parser = new AutoDetectParser();
		Metadata metadata = new Metadata();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ToHTMLContentHandler handler = new ToHTMLContentHandler(output, StandardCharsets.UTF_8.name());
		ParseContext context = new ParseContext();
		// EmbeddedDocumentExtractor extractor = new ImageExtractor(context, imageDir);

		// context.set(EmbeddedDocumentExtractor.class, extractor);

		try (InputStream stream = TikaInputStream.get(file.toPath())) {
			parser.parse(stream, handler, metadata, context);
		}

		String html = output.toString(StandardCharsets.UTF_8);

		return HtmlToMarkdown.convert(html);
	}

}
