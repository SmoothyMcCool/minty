package tom.document.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cleans markdown output produced by pandoc from Word documents.
 *
 * Cleaning passes (applied in this order):
 *
 * 1. Smart quotes — curly/typographic quote chars → straight ASCII quotes 2.
 * Dashes — em dash (—) and en dash (–) → hyphen-minus (-) 3. Non-breaking space
 * — U+00A0 → regular space 4. Soft hyphen — U+00AD (invisible) → removed
 * entirely 5. Broken lists — "1 text" style items → "1. text", renumbered from
 * 1 6. Heading spacing — "# Title" (extra spaces after hashes) → "# Title" 7.
 * Normalize — trailing whitespace, excess blank lines, trim
 */
public class MarkdownCleaner {

	// -------------------------------------------------------------------------
	// Pass 1 — Smart quotes
	// -------------------------------------------------------------------------

	// Double curly quotes → straight double quote
	private static final Pattern OPEN_DOUBLE_QUOTE = Pattern.compile("\u201C");
	private static final Pattern CLOSE_DOUBLE_QUOTE = Pattern.compile("\u201D");

	// Single curly quotes / apostrophe variants → straight single quote
	private static final Pattern OPEN_SINGLE_QUOTE = Pattern.compile("\u2018");
	private static final Pattern CLOSE_SINGLE_QUOTE = Pattern.compile("\u2019");

	// -------------------------------------------------------------------------
	// Pass 2 — Dashes
	// -------------------------------------------------------------------------

	// Em dash (—) and en dash (–) → hyphen-minus
	private static final Pattern EM_DASH = Pattern.compile("\u2014");
	private static final Pattern EN_DASH = Pattern.compile("\u2013");

	// -------------------------------------------------------------------------
	// Pass 5 — Broken ordered lists
	// -------------------------------------------------------------------------

	/**
	 * Matches a broken list item: optional indent, a number, one or more spaces
	 * (but no dot), then item text. Does NOT match already-correct "1. text".
	 *
	 * Groups: 1 — leading indent (may be empty) 2 — number (discarded; we renumber)
	 * 3 — item text
	 */
	private static final Pattern LIST_ITEM = Pattern.compile("^([ \\t]*)(\\d+)(?:\\.|[ \\t]+)(.+)$");
	private static final Pattern LIST_SEPARATOR = Pattern.compile("^\\s*<!--.*?-->\\s*$");

	// -------------------------------------------------------------------------
	// Pass 6 — Heading spacing
	// -------------------------------------------------------------------------

	// "## Title" → "## Title" (one or more hashes, then 2+ spaces)
	private static final Pattern HEADING_EXTRA_SPACE = Pattern.compile("^(#{1,6})[ \\t]{2,}", Pattern.MULTILINE);

	// -------------------------------------------------------------------------
	// Public entry point
	// -------------------------------------------------------------------------

	/**
	 * Runs all cleaning passes and returns the cleaned markdown.
	 */
	public static String clean(String markdown) {
		String s = markdown;
		s = fixSmartQuotes(s);
		s = fixDashes(s);
		s = fixNonBreakingSpaces(s);
		s = fixSoftHyphens(s);
		s = fixBrokenLists(s);
		s = removeHtmlComments(s);
		s = fixHeadingSpacing(s);
		s = normalize(s);
		return s;
	}

	// -------------------------------------------------------------------------
	// Pass 1 — Smart quotes
	// -------------------------------------------------------------------------

	static String fixSmartQuotes(String s) {
		String ret = OPEN_DOUBLE_QUOTE.matcher(s).replaceAll("\"");
		ret = CLOSE_DOUBLE_QUOTE.matcher(ret).replaceAll("\"");
		ret = OPEN_SINGLE_QUOTE.matcher(ret).replaceAll("'");
		ret = CLOSE_SINGLE_QUOTE.matcher(ret).replaceAll("'");
		return ret;
	}

	// -------------------------------------------------------------------------
	// Pass 2 — Dashes
	// -------------------------------------------------------------------------

	static String fixDashes(String s) {
		String ret = EM_DASH.matcher(s).replaceAll("-");
		ret = EN_DASH.matcher(ret).replaceAll("-");
		return ret;
	}

	// -------------------------------------------------------------------------
	// Pass 3 — Non-breaking spaces
	// -------------------------------------------------------------------------

	static String fixNonBreakingSpaces(String s) {
		// U+00A0 non-breaking space → regular space
		return s.replace('\u00A0', ' ');
	}

	// -------------------------------------------------------------------------
	// Pass 4 — Soft hyphens
	// -------------------------------------------------------------------------

	static String fixSoftHyphens(String s) {
		// U+00AD soft hyphen → remove entirely (invisible in Word, meaningless in
		// markdown)
		return s.replace("\u00AD", "");
	}

	// -------------------------------------------------------------------------
	// Pass 5 — Broken ordered lists
	// -------------------------------------------------------------------------

	/**
	 * Scans line by line. Contiguous runs of broken list items are collected and
	 * re-emitted as properly numbered markdown ordered lists.
	 *
	 * A single blank line between broken items is treated as inter-item spacing
	 * (Word lists commonly have this). Two or more consecutive blank lines
	 * terminate the current list block.
	 */
	static String fixBrokenLists(String markdown) {
		String[] lines = markdown.split("\n", -1);
		StringBuilder out = new StringBuilder();

		List<String> pendingItems = new ArrayList<>();
		String pendingIndent = "";
		int blankBuffer = 0;

		for (String line : lines) {
			Matcher m = LIST_ITEM.matcher(line);

			if (m.matches()) {
				String indent = m.group(1);
				String text = m.group(3).trim();

				if (pendingItems.isEmpty()) {
					appendBlanks(out, blankBuffer);
					blankBuffer = 0;
					pendingIndent = indent;
				} else {
					for (int i = 0; i < blankBuffer; i++) {
						pendingItems.add(""); // blank-line sentinel inside list
					}
					blankBuffer = 0;
				}
				pendingItems.add(text);

			} else if (line.isBlank() || LIST_SEPARATOR.matcher(line).matches()) {
				if (!pendingItems.isEmpty()) {
					// treat separator as intra-list spacing
					blankBuffer++;
				} else {
					// outside a list, preserve as normal blank line
					out.append("\n");
				}
			} else {
				if (!pendingItems.isEmpty()) {
					flushBlock(out, pendingItems, pendingIndent);
					pendingItems.clear();
					appendBlanks(out, blankBuffer);
					blankBuffer = 0;
					pendingIndent = "";
				} else {
					appendBlanks(out, blankBuffer);
					blankBuffer = 0;
				}
				out.append(line).append("\n");
			}
		}

		// EOF flush
		if (!pendingItems.isEmpty()) {
			flushBlock(out, pendingItems, pendingIndent);
			appendBlanks(out, blankBuffer);
		} else {
			appendBlanks(out, blankBuffer);
		}

		return out.toString();
	}

	static String removeHtmlComments(String s) {
		// Remove standalone HTML comment lines (like <!-- -->)
		return s.replaceAll("(?m)^\\s*<!--.*?-->\\s*\\n?", "");
	}

	private static void flushBlock(StringBuilder out, List<String> items, String indent) {
		int counter = 1;
		for (String item : items) {
			if (item.isEmpty()) {
				out.append("\n");
			} else {
				out.append(indent).append(counter++).append(". ").append(item).append("\n");
			}
		}
	}

	private static void appendBlanks(StringBuilder out, int count) {
		for (int i = 0; i < count; i++) {
			out.append("\n");
		}
	}

	// -------------------------------------------------------------------------
	// Pass 6 — Heading spacing
	// -------------------------------------------------------------------------

	static String fixHeadingSpacing(String s) {
		// Replace "## Title" → "## Title"
		return HEADING_EXTRA_SPACE.matcher(s).replaceAll("$1 ");
	}

	// -------------------------------------------------------------------------
	// Pass 7 — Normalize whitespace
	// -------------------------------------------------------------------------

	static String normalize(String markdown) {
		return markdown.replaceAll("[ \\t]+\\n", "\n") // trailing whitespace on lines
				.replaceAll("\\n{3,}", "\n\n") // collapse 3+ blank lines to 1
				.trim() + "\n";
	}
}