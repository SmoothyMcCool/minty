package tom.document.service.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.extractor.ParsingEmbeddedDocumentExtractor;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ImageExtractor implements EmbeddedDocumentExtractor {

	private final EmbeddedDocumentExtractor delegate;
	private final Path imageDir;
	private final AtomicInteger counter = new AtomicInteger();

	public ImageExtractor(ParseContext context, Path imageDir) {
		this.delegate = new ParsingEmbeddedDocumentExtractor(context);
		this.imageDir = imageDir;
	}

	@Override
	public boolean shouldParseEmbedded(Metadata metadata) {
		return true;
	}

	@Override
	public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
			throws SAXException, IOException {

		String contentType = metadata.get(HttpHeaders.CONTENT_TYPE);

		if (contentType != null && contentType.startsWith("image/")) {

			int id = counter.incrementAndGet();
			String ext = contentType.substring(contentType.indexOf('/') + 1);

			Path file = imageDir.resolve("image-" + id + "." + ext);

			Files.copy(stream, file, StandardCopyOption.REPLACE_EXISTING);

		} else {

			delegate.parseEmbedded(stream, handler, metadata, outputHtml);

		}
	}
}