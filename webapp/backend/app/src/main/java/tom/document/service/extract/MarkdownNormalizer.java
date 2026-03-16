package tom.document.service.extract;

public class MarkdownNormalizer {

	public static String normalize(String markdown) {

		// Render to HTML then convert back to markdown if desired
		// but most pipelines simply accept normalized HTML or keep markdown.

		return markdown.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").trim() + "\n";
	}
}