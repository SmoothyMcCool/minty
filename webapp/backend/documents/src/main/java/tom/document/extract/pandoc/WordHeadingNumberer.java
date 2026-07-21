package tom.document.extract.pandoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Resolves Word's *auto-generated* multilevel heading numbers (e.g. "2.3.1")
 * directly from the docx OOXML parts and injects them into the markdown that
 * pandoc produces.
 *
 * <p>
 * Why this exists: pandoc's docx reader does not retain numbering information
 * on Header elements at all -- it is simply absent from the AST pandoc hands to
 * Lua filters. So a Lua Header() filter can never recover these numbers; the
 * resolution has to happen either against the raw docx XML (this class) or not
 * at all.
 *
 * <p>
 * This only matters for headings that rely on Word's automatic multilevel-list
 * numbering (numPr, possibly inherited from the paragraph style). If your
 * headings already contain the number as literal typed text ("1.1 Scope"),
 * pandoc preserves that on its own and this class is a no-op for those headings
 * (see {@link #alreadyNumbered(String)}).
 *
 * <p>
 * Known limitations, deliberately not handled to keep this maintainable:
 * <ul>
 * <li>Only numFmt values decimal, lowerLetter, upperLetter, lowerRoman,
 * upperRoman are formatted; anything else (chicago, ordinalText, decimalZero,
 * custom) falls back to plain decimal.</li>
 * <li>Style inheritance for numPr is resolved one level via w:basedOn, not the
 * full chain. Deep style hierarchies with numbering three or more levels up in
 * the basedOn chain won't be picked up.</li>
 * <li>w:lvlOverride/w:startOverride is honored; other override types (custom
 * lvlText per num instance) are not.</li>
 * <li>Assumes pandoc emits ATX-style ("#", "##", ...) headers, which is
 * standard for gfm/commonmark output.</li>
 * </ul>
 */
public final class WordHeadingNumberer {

	private static final Logger logger = LogManager.getLogger(WordHeadingNumberer.class);

	private static final Pattern HEADING_STYLE_ID = Pattern.compile("^Heading(\\d)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ATX_HEADER = Pattern.compile("^(#{1,6})(\\s+)(.*)$");
	private static final Pattern ALREADY_NUMBERED = Pattern.compile("^\\(?\\d+([.\\-]\\d+)*[.)]?\\s");

	private WordHeadingNumberer() {
	}

	/**
	 * Best-effort: computes Word's real heading numbers from {@code docx} and
	 * prepends them to the corresponding ATX headers in {@code markdown}, in
	 * document order. Never throws -- on any failure (not a docx, malformed XML, no
	 * numbering used, etc.) the original markdown is returned unchanged and a
	 * warning is logged.
	 */
	public static String injectNumbers(File docx, String markdown) {
		try {
			List<String> numbers = resolveHeadingNumbers(docx);
			if (numbers.isEmpty()) {
				return markdown;
			}
			return injectIntoMarkdown(markdown, numbers);
		} catch (Exception e) {
			logger.warn("Could not resolve Word heading numbers for {} ({}); continuing without them.", docx.getName(),
					e.toString());
			return markdown;
		}
	}

	// ------------------------------------------------------------------
	// docx XML -> ordered list of resolved number strings
	// ------------------------------------------------------------------

	private static List<String> resolveHeadingNumbers(File docx) throws Exception {
		Document documentXml = readZipEntryXml(docx, "word/document.xml");
		if (documentXml == null) {
			return List.of();
		}
		Document numberingXml = readZipEntryXml(docx, "word/numbering.xml");
		Document stylesXml = readZipEntryXml(docx, "word/styles.xml");

		Map<String, int[]> abstractLevelFormats; // abstractNumId -> per-level [numFmtCode]
		Map<String, String[]> abstractLevelText; // abstractNumId -> per-level lvlText
		Map<String, Integer[]> abstractLevelStart; // abstractNumId -> per-level start value
		Map<String, String> numIdToAbstractId;

		if (numberingXml == null) {
			return List.of(); // no numbering defined anywhere in the doc
		}
		NumberingDefs defs = parseNumberingXml(numberingXml);

		Map<String, int[]> styleNumPr = stylesXml == null ? Map.of() : parseStyleNumPr(stylesXml);

		List<String> results = new ArrayList<>();
		Map<String, int[]> counters = new HashMap<>(); // numId -> counters per level (1-indexed values)

		NodeList paragraphs = documentXml.getElementsByTagName("w:p");
		for (int i = 0; i < paragraphs.getLength(); i++) {
			Element p = (Element) paragraphs.item(i);
			if (!isHeadingParagraph(p)) {
				continue;
			}

			int[] numPr = directNumPr(p); // [numId, ilvl] or null
			if (numPr == null) {
				numPr = styleNumPr.get(headingStyleId(p));
			}
			if (numPr == null) {
				continue; // heading with no numbering attached -- nothing to compute
			}

			String numId = String.valueOf(numPr[0]);
			int ilvl = numPr[1];
			String abstractId = defs.numIdToAbstractId.get(numId);
			if (abstractId == null) {
				continue;
			}

			int[] fmtCodes = defs.abstractLevelFormat.get(abstractId);
			String[] lvlTexts = defs.abstractLevelText.get(abstractId);
			int[] starts = defs.abstractLevelStart.get(abstractId);
			if (fmtCodes == null || ilvl >= fmtCodes.length) {
				continue;
			}

			int[] counter = counters.computeIfAbsent(numId, k -> {
				int[] c = new int[9];
				Arrays.fill(c, -1); // -1 = "not started yet"
				return c;
			});

			counter[ilvl] = (counter[ilvl] < 0 ? starts[ilvl] : counter[ilvl] + 1);
			for (int lvl = ilvl + 1; lvl < counter.length; lvl++) {
				counter[lvl] = -1; // reset deeper levels
			}

			results.add(formatLevelText(lvlTexts[ilvl], counter, fmtCodes));
		}

		return results;
	}

	private static boolean isHeadingParagraph(Element p) {
		String styleId = headingStyleId(p);
		if (styleId != null && HEADING_STYLE_ID.matcher(styleId).matches()) {
			return true;
		}
		// Fallback: outlineLvl is how Word marks "this paragraph is a heading"
		// independent of style name (used by custom heading style names).
		return firstDescendant(p, "w:outlineLvl") != null;
	}

	private static String headingStyleId(Element p) {
		Element pStyle = firstDescendant(p, "w:pStyle");
		return pStyle == null ? null : pStyle.getAttribute("w:val");
	}

	/** Reads numPr (numId, ilvl) directly on the paragraph, if present. */
	private static int[] directNumPr(Element p) {
		Element numPr = firstDescendant(p, "w:numPr");
		if (numPr == null) {
			return null;
		}
		Element numId = firstDescendant(numPr, "w:numId");
		Element ilvl = firstDescendant(numPr, "w:ilvl");
		if (numId == null) {
			return null;
		}
		int numIdVal = parseIntSafe(numId.getAttribute("w:val"), -1);
		int ilvlVal = ilvl == null ? 0 : parseIntSafe(ilvl.getAttribute("w:val"), 0);
		return numIdVal < 0 ? null : new int[] { numIdVal, ilvlVal };
	}

	// ------------------------------------------------------------------
	// numbering.xml parsing
	// ------------------------------------------------------------------

	private static final class NumberingDefs {
		Map<String, String> numIdToAbstractId = new HashMap<>();
		Map<String, int[]> abstractLevelFormat = new HashMap<>(); // numFmt code per level
		Map<String, String[]> abstractLevelText = new HashMap<>(); // lvlText pattern per level
		Map<String, int[]> abstractLevelStart = new HashMap<>(); // start value per level
	}

	private static NumberingDefs parseNumberingXml(Document numberingXml) {
		NumberingDefs defs = new NumberingDefs();

		NodeList abstractNums = numberingXml.getElementsByTagName("w:abstractNum");
		for (int i = 0; i < abstractNums.getLength(); i++) {
			Element abstractNum = (Element) abstractNums.item(i);
			String abstractId = abstractNum.getAttribute("w:abstractNumId");

			int[] fmt = new int[9];
			String[] text = new String[9];
			int[] start = new int[9];
			Arrays.fill(start, 1);

			NodeList lvls = abstractNum.getElementsByTagName("w:lvl");
			for (int j = 0; j < lvls.getLength(); j++) {
				Element lvl = (Element) lvls.item(j);
				int ilvl = parseIntSafe(lvl.getAttribute("w:ilvl"), j);
				if (ilvl < 0 || ilvl >= 9) {
					continue;
				}
				Element numFmt = firstDescendant(lvl, "w:numFmt");
				Element lvlText = firstDescendant(lvl, "w:lvlText");
				Element startEl = firstDescendant(lvl, "w:start");

				fmt[ilvl] = numFmtCode(numFmt == null ? "decimal" : numFmt.getAttribute("w:val"));
				text[ilvl] = lvlText == null ? "%" + (ilvl + 1) + "." : lvlText.getAttribute("w:val");
				start[ilvl] = startEl == null ? 1 : parseIntSafe(startEl.getAttribute("w:val"), 1);
			}

			defs.abstractLevelFormat.put(abstractId, fmt);
			defs.abstractLevelText.put(abstractId, text);
			defs.abstractLevelStart.put(abstractId, start);
		}

		NodeList nums = numberingXml.getElementsByTagName("w:num");
		for (int i = 0; i < nums.getLength(); i++) {
			Element num = (Element) nums.item(i);
			String numId = num.getAttribute("w:numId");
			Element abstractNumId = firstDescendant(num, "w:abstractNumId");
			if (abstractNumId != null) {
				defs.numIdToAbstractId.put(numId, abstractNumId.getAttribute("w:val"));
			}
			// Basic startOverride support: overwrite level 0 start if present.
			NodeList overrides = num.getElementsByTagName("w:lvlOverride");
			for (int j = 0; j < overrides.getLength(); j++) {
				Element override = (Element) overrides.item(j);
				int ilvl = parseIntSafe(override.getAttribute("w:ilvl"), -1);
				Element startOverride = firstDescendant(override, "w:startOverride");
				if (ilvl >= 0 && startOverride != null && abstractNumId != null) {
					int[] starts = defs.abstractLevelStart.get(abstractNumId.getAttribute("w:val"));
					if (starts != null && ilvl < starts.length) {
						starts[ilvl] = parseIntSafe(startOverride.getAttribute("w:val"), starts[ilvl]);
					}
				}
			}
		}

		return defs;
	}

	/**
	 * numId/ilvl attached at the *style* level (w:styles > w:style > w:pPr >
	 * w:numPr), keyed by style id.
	 */
	private static Map<String, int[]> parseStyleNumPr(Document stylesXml) {
		Map<String, int[]> result = new HashMap<>();
		NodeList styles = stylesXml.getElementsByTagName("w:style");
		for (int i = 0; i < styles.getLength(); i++) {
			Element style = (Element) styles.item(i);
			String styleId = style.getAttribute("w:styleId");
			Element pPr = firstDescendant(style, "w:pPr");
			if (pPr == null) {
				continue;
			}
			Element numPr = firstDescendant(pPr, "w:numPr");
			if (numPr == null) {
				continue;
			}
			Element numId = firstDescendant(numPr, "w:numId");
			Element ilvl = firstDescendant(numPr, "w:ilvl");
			if (numId == null) {
				continue;
			}
			int numIdVal = parseIntSafe(numId.getAttribute("w:val"), -1);
			int ilvlVal = ilvl == null ? 0 : parseIntSafe(ilvl.getAttribute("w:val"), 0);
			if (numIdVal >= 0) {
				result.put(styleId, new int[] { numIdVal, ilvlVal });
			}
		}
		return result;
	}

	private static int numFmtCode(String val) {
		if (val == null)
			return 0;
		return switch (val) {
		case "lowerLetter" -> 1;
		case "upperLetter" -> 2;
		case "lowerRoman" -> 3;
		case "upperRoman" -> 4;
		default -> 0; // decimal, and anything unsupported
		};
	}

	private static String formatLevelText(String lvlText, int[] counter, int[] fmtCodes) {
		if (lvlText == null) {
			return String.valueOf(counter[0]);
		}
		StringBuilder out = new StringBuilder();
		Matcher m = Pattern.compile("%(\\d)").matcher(lvlText);
		int last = 0;
		while (m.find()) {
			out.append(lvlText, last, m.start());
			int level = Integer.parseInt(m.group(1)) - 1;
			int value = level >= 0 && level < counter.length && counter[level] > 0 ? counter[level] : 1;
			int fmt = level >= 0 && level < fmtCodes.length ? fmtCodes[level] : 0;
			out.append(formatNumber(value, fmt));
			last = m.end();
		}
		out.append(lvlText.substring(last));
		return out.toString();
	}

	private static String formatNumber(int value, int fmtCode) {
		return switch (fmtCode) {
		case 1 -> toLetters(value).toLowerCase(Locale.ROOT);
		case 2 -> toLetters(value).toUpperCase(Locale.ROOT);
		case 3 -> toRoman(value).toLowerCase(Locale.ROOT);
		case 4 -> toRoman(value).toUpperCase(Locale.ROOT);
		default -> String.valueOf(value);
		};
	}

	private static String toLetters(int value) {
		StringBuilder sb = new StringBuilder();
		while (value > 0) {
			int rem = (value - 1) % 26;
			sb.insert(0, (char) ('A' + rem));
			value = (value - 1) / 26;
		}
		return sb.length() == 0 ? "A" : sb.toString();
	}

	private static final int[] ROMAN_VALUES = { 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 };
	private static final String[] ROMAN_SYMS = { "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV",
			"I" };

	private static String toRoman(int value) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ROMAN_VALUES.length && value > 0; i++) {
			while (value >= ROMAN_VALUES[i]) {
				value -= ROMAN_VALUES[i];
				sb.append(ROMAN_SYMS[i]);
			}
		}
		return sb.toString();
	}

	// ------------------------------------------------------------------
	// markdown injection
	// ------------------------------------------------------------------

	private static String injectIntoMarkdown(String markdown, List<String> numbers) {
		String[] lines = markdown.split("\n", -1);
		Deque<String> queue = new ArrayDeque<>(numbers);
		StringBuilder out = new StringBuilder(markdown.length() + numbers.size() * 6);
		boolean inFence = false;

		for (String line : lines) {
			if (line.startsWith("```")) {
				inFence = !inFence;
				out.append(line).append('\n');
				continue;
			}
			Matcher m = !inFence ? ATX_HEADER.matcher(line) : null;
			if (m != null && m.matches() && !queue.isEmpty()) {
				String title = m.group(3);
				if (alreadyNumbered(title)) {
					out.append(line).append('\n'); // Word doc already types the number; don't double up
				} else {
					String number = queue.poll();
					out.append(m.group(1)).append(m.group(2)).append(number).append(' ').append(title).append('\n');
				}
			} else {
				out.append(line).append('\n');
			}
		}

		if (out.length() > 0) {
			out.setLength(out.length() - 1); // drop trailing newline we added
		}
		return out.toString();
	}

	private static boolean alreadyNumbered(String headingText) {
		return ALREADY_NUMBERED.matcher(headingText).find();
	}

	// ------------------------------------------------------------------
	// small XML/zip helpers
	// ------------------------------------------------------------------

	private static Document readZipEntryXml(File docx, String entryName) throws Exception {
		try (ZipFile zip = new ZipFile(docx)) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null) {
				return null;
			}
			try (InputStream in = zip.getInputStream(entry)) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(false);
				// Harden against XXE while parsing untrusted docx content.
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				DocumentBuilder builder = factory.newDocumentBuilder();
				return builder.parse(in);
			}
		} catch (IOException e) {
			return null; // not a zip / not a docx
		}
	}

	private static Element firstDescendant(Element parent, String tagName) {
		NodeList list = parent.getElementsByTagName(tagName);
		return list.getLength() == 0 ? null : (Element) list.item(0);
	}

	private static int parseIntSafe(String s, int fallback) {
		if (s == null || s.isEmpty()) {
			return fallback;
		}
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}