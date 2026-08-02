package tom.document.extract.pandoc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Resolves Word's multilevel heading numbers (e.g. "2.3.1") for OOXML files.
 *
 * HYBRID MODE: 1. If numbering metadata exists (Word/LibreOffice), it
 * calculates complex hierarchical numbers. 2. If no numbering metadata exists
 * (Google Docs), it performs sequential numbering (1., 2., 3...) on all
 * detected headings to ensure the Markdown is numbered.
 */
public final class WordHeadingNumberer {

	private static final Logger logger = LogManager.getLogger(WordHeadingNumberer.class);

	private static final Pattern HEADING_STYLE_ID = Pattern.compile("^Heading\\s*(\\d)$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ATX_HEADER = Pattern.compile("^(#{1,6})(\\s+)(.*)$");
	private static final Pattern ALREADY_NUMBERED = Pattern.compile("^\\(?\\d+([.\\-]\\d+)*[.)]?\\s");

	private WordHeadingNumberer() {
	}

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

		Map<String, Element> stylesById = stylesXml == null ? Map.of() : indexStylesById(stylesXml);

		// --- STEP 1: Check if we are in a "Rich" numbering document (Word/LibreOffice)
		// ---
		boolean hasComplexNumbering = false;
		if (numberingXml != null) {
			NodeList abstractNums = numberingXml.getElementsByTagNameNS("*", "abstractNum");
			if (abstractNums.getLength() > 0) {
				hasComplexNumbering = true;
			}
		}

		// --- CASE A: COMPLEX MODE (Word/LibreOffice) ---
		if (hasComplexNumbering && numberingXml != null) {
			return computeComplexNumbers(documentXml, numberingXml, stylesById);
		}

		// --- CASE B: SEQUENTIAL FALLBACK MODE (Google Docs / Flat XML) ---
		else {
			logger.info("WordHeadingNumberer [{}]: Using hierarchical sequential numbering.", docx.getName());
			List<String> sequentialNumbers = new ArrayList<>();
			NodeList paragraphs = documentXml.getElementsByTagNameNS("*", "p");

			// We'll use a list of counters for the fallback (e.g., [1, 0, 0] means 1.0.0)
			int[] currentCounters = new int[6]; // Support up to Level 6
			int lastLevel = 0;

			for (int i = 0; i < paragraphs.getLength(); i++) {
				Element p = (Element) paragraphs.item(i);
				if (!isHeadingParagraph(p, stylesById))
					continue;

				// Determine the level of this heading by looking at its style or properties
				int currentLevel = getHeadingLevelFromElement(p, stylesById);
				if (currentLevel == 0)
					continue;

				String title = getFirstTextContent(p);
				if (!alreadyNumbered(title)) {
					// Update our counters based on the level transition
					// If we go from Level 1 to Level 3, reset levels 2 and 3.
					for (int l = currentLevel; l < 6; l++)
						currentCounters[l] = 0;
					currentCounters[currentLevel - 1]++;

					// Build the string: e.g., "1.2"
					StringBuilder sb = new StringBuilder();
					for (int l = 0; l <= currentLevel - 1; l++) {
						if (l > 0)
							sb.append(".");
						sb.append(currentCounters[l]);
					}
					sequentialNumbers.add(sb.toString());
				}
			}
			return sequentialNumbers;
		}
	}

	private static int getHeadingLevelFromElement(Element p, Map<String, Element> stylesById) {
		// 1. Try to find via w:outlineLvl (The most reliable way in OOXML)
		Element outline = firstDescendant(p, "outlineLvl");
		if (outline != null) {
			int lvl = parseIntSafe(outline.getAttribute("w:val"), 0);
			return lvl + 1; // XML is 0-indexed, Markdown is 1-indexed
		}

		// 2. Try via the style name (e.g., "Heading 2")
		String styleId = headingStyleId(p);
		if (styleId != null) {
			Matcher m = Pattern.compile("(?i)Heading\\s*(\\d)").matcher(styleId);
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		}

		// 3. Default to Level 1 if we can't determine it, but only if it looks like a
		// heading
		return 1;
	}

	/**
	 * Logic for resolving hierarchical numbers (1.1.2) using numbering.xml
	 */
	private static List<String> computeComplexNumbers(Document documentXml, Document numberingXml,
			Map<String, Element> stylesById) throws Exception {
		NumberingDefs defs = parseNumberingXml(numberingXml);
		List<String> results = new ArrayList<>();
		Map<String, int[]> counters = new HashMap<>();

		NodeList paragraphs = documentXml.getElementsByTagNameNS("*", "p");

		for (int i = 0; i < paragraphs.getLength(); i++) {
			Element p = (Element) paragraphs.item(i);
			if (!isHeadingParagraph(p, stylesById))
				continue;

			int[] numPrArr = directNumPr(p);
			if (numPrArr == null) {
				numPrArr = resolveNumPrViaStyleChain(headingStyleId(p), stylesById);
			}
			if (numPrArr == null)
				continue;

			String numIdStr = String.valueOf(numPrArr[0]);
			int ilvl = numPrArr[1];
			String abstractId = defs.numIdToAbstractId.get(numIdStr);
			if (abstractId == null)
				continue;

			int[] fmtCodes = defs.abstractLevelFormat.get(abstractId);
			String[] lvlTexts = defs.abstractLevelText.get(abstractId);
			int[] starts = defs.abstractLevelStart.get(abstractId);
			if (fmtCodes == null || ilvl >= fmtCodes.length)
				continue;

			int[] counter = counters.computeIfAbsent(numIdStr, k -> {
				int[] c = new int[9];
				Arrays.fill(c, -1);
				return c;
			});

			counter[ilvl] = (counter[ilvl] < 0 ? starts[ilvl] : counter[ilvl] + 1);
			for (int lvl = ilvl + 1; lvl < counter.length; lvl++) {
				counter[lvl] = -1;
			}

			results.add(formatLevelText(lvlTexts[ilvl], counter, fmtCodes));
		}
		return results;
	}

	// ------------------------------------------------------------------
	// XML Parsing Helpers (Namespace Aware)
	// ------------------------------------------------------------------

	private static final class NumberingDefs {
		Map<String, String> numIdToAbstractId = new HashMap<>();
		Map<String, int[]> abstractLevelFormat = new HashMap<>();
		Map<String, String[]> abstractLevelText = new HashMap<>();
		Map<String, int[]> abstractLevelStart = new HashMap<>();
	}

	private static NumberingDefs parseNumberingXml(Document numberingXml) {
		NumberingDefs defs = new NumberingDefs();
		NodeList abstractNums = numberingXml.getElementsByTagNameNS("*", "abstractNum");

		for (int i = 0; i < abstractNums.getLength(); i++) {
			Element abstractNum = (Element) abstractNums.item(i);
			String abstractId = abstractNum.getAttribute("w:abstractNumId");

			int[] fmt = new int[9];
			String[] text = new String[9];
			int[] start = new int[9];
			Arrays.fill(start, 1);

			NodeList lvls = abstractNum.getElementsByTagNameNS("*", "lvl");
			for (int j = 0; j < lvls.getLength(); j++) {
				Element lvl = (Element) lvls.item(j);
				int ilvl = parseIntSafe(lvl.getAttribute("w:ilvl"), j);
				if (ilvl < 0 || ilvl >= 9)
					continue;

				Element numFmt = firstDescendant(lvl, "numFmt");
				Element lvlText = firstDescendant(lvl, "lvlText");
				Element startEl = firstDescendant(lvl, "start");

				fmt[ilvl] = numFmtCode(numFmt == null ? "decimal" : numFmt.getAttribute("w:val"));
				String lvlTextVal = lvlText == null ? null : lvlText.getAttribute("w:val");
				text[ilvl] = (lvlTextVal == null || lvlTextVal.isEmpty()) ? cumulativeLevelPattern(ilvl) : lvlTextVal;
				start[ilvl] = startEl == null ? 1 : parseIntSafe(startEl.getAttribute("w:val"), 1);
			}

			defs.abstractLevelFormat.put(abstractId, fmt);
			defs.abstractLevelText.put(abstractId, text);
			defs.abstractLevelStart.put(abstractId, start);
		}

		NodeList nums = numberingXml.getElementsByTagNameNS("*", "num");
		for (int i = 0; i < nums.getLength(); i++) {
			Element num = (Element) nums.item(i);
			String numId = num.getAttribute("w:numId");
			Element abstractNumIdElem = firstDescendant(num, "abstractNumId");
			if (abstractNumIdElem != null) {
				defs.numIdToAbstractId.put(numId, abstractNumIdElem.getAttribute("w:val"));
			}
		}
		return defs;
	}

	private static Map<String, Element> indexStylesById(Document stylesXml) {
		Map<String, Element> map = new HashMap<>();
		NodeList styles = stylesXml.getElementsByTagNameNS("*", "style");
		for (int i = 0; i < styles.getLength(); i++) {
			Element style = (Element) styles.item(i);
			String id = style.getAttribute("w:styleId");
			if (id != null && !id.isEmpty())
				map.put(id, style);
		}
		return map;
	}

	private static boolean isHeadingParagraph(Element p, Map<String, Element> stylesById) {
		String styleId = headingStyleId(p);
		if (styleId != null && HEADING_STYLE_ID.matcher(styleId).matches())
			return true;
		if (firstDescendant(p, "outlineLvl") != null)
			return true;
		return resolveViaStyleChain(styleId, stylesById, pPr -> firstDescendant(pPr, "outlineLvl")) != null;
	}

	private static String headingStyleId(Element p) {
		Element pStyle = firstDescendant(p, "pStyle");
		return pStyle == null ? null : pStyle.getAttribute("w:val");
	}

	private static int[] directNumPr(Element p) {
		Element numPr = firstDescendant(p, "numPr");
		return numPr == null ? null : readNumPr(numPr);
	}

	private static int[] resolveNumPrViaStyleChain(String styleId, Map<String, Element> stylesById) {
		Element numPr = resolveViaStyleChain(styleId, stylesById, pPr -> firstDescendant(pPr, "numPr"));
		return numPr == null ? null : readNumPr(numPr);
	}

	private static int[] readNumPr(Element numPr) {
		Element numId = firstDescendant(numPr, "numId");
		Element ilvl = firstDescendant(numPr, "ilvl");
		if (numId == null)
			return null;
		int numIdVal = parseIntSafe(numId.getAttribute("w:val"), -1);
		int ilvlVal = ilvl == null ? 0 : parseIntSafe(ilvl.getAttribute("w:val"), 0);
		return numIdVal < 0 ? null : new int[] { numIdVal, ilvlVal };
	}

	private static Element resolveViaStyleChain(String styleId, Map<String, Element> stylesById,
			java.util.function.Function<Element, Element> lookup) {
		Set<String> visited = new HashSet<>();
		while (styleId != null && visited.add(styleId)) {
			Element style = stylesById.get(styleId);
			if (style == null)
				return null;
			Element pPr = firstDescendant(style, "pPr");
			if (pPr != null) {
				Element found = lookup.apply(pPr);
				if (found != null)
					return found;
			}
			Element basedOn = firstDescendant(style, "basedOn");
			styleId = basedOn == null ? null : basedOn.getAttribute("w:val");
		}
		return null;
	}

	private static String cumulativeLevelPattern(int ilvl) {
		StringBuilder sb = new StringBuilder();
		for (int lvl = 1; lvl <= ilvl + 1; lvl++) {
			if (lvl > 1)
				sb.append('.');
			sb.append('%').append(lvl);
		}
		return sb.toString();
	}

	private static int numFmtCode(String val) {
		if (val == null)
			return 0;
		return switch (val) {
		case "lowerLetter" -> 1;
		case "upperLetter" -> 2;
		case "lowerRoman" -> 3;
		case "upperRoman" -> 4;
		default -> 0;
		};
	}

	private static String formatLevelText(String lvlText, int[] counter, int[] fmtCodes) {
		if (lvlText == null)
			return String.valueOf(counter[0]);
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
		String[] lines = markdown.split("\\r?\\n", -1);
		Deque<String> queue = new ArrayDeque<>(numbers);
		StringBuilder out = new StringBuilder();
		boolean inFence = false;

		for (String line : lines) {
			if (line.startsWith("```")) {
				inFence = !inFence;
				out.append(line).append('\n');
				continue;
			}
			Matcher m = !inFence ? ATX_HEADER.matcher(line.trim()) : null;
			if (m != null && m.matches() && !queue.isEmpty()) {
				String title = m.group(3);
				if (alreadyNumbered(title)) {
					out.append(line).append('\n');
				} else {
					String number = queue.poll();
					out.append(m.group(1)).append(m.group(2)).append(number).append(" ").append(title).append('\n');
				}
			} else {
				out.append(line).append('\n');
			}
		}

		if (out.length() > 0)
			out.setLength(out.length() - 1);
		return out.toString();
	}

	private static boolean alreadyNumbered(String headingText) {
		return ALREADY_NUMBERED.matcher(headingText).find();
	}

	// ------------------------------------------------------------------
	// XML/zip helpers
	// ------------------------------------------------------------------

	private static Document readZipEntryXml(File docx, String entryName) throws Exception {
		try (ZipFile zip = new ZipFile(docx)) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null)
				return null;
			try (InputStream in = zip.getInputStream(entry)) {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
				return factory.newDocumentBuilder().parse(in);
			}
		} catch (IOException e) {
			return null;
		}
	}

	private static Element firstDescendant(Element parent, String localName) {
		NodeList list = parent.getElementsByTagNameNS("*", getLocalName(localName));
		return list.getLength() == 0 ? null : (Element) list.item(0);
	}

	private static String getLocalName(String nameWithPrefix) {
		int index = nameWithPrefix.indexOf(':');
		return (index == -1) ? nameWithPrefix : nameWithPrefix.substring(index + 1);
	}

	private static int parseIntSafe(String s, int fallback) {
		if (s == null || s.isEmpty())
			return fallback;
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private static String getFirstTextContent(Element parent) {
		NodeList nl = parent.getElementsByTagNameNS("*", "t");
		if (nl.getLength() > 0)
			return nl.item(0).getTextContent().trim();
		return "";
	}
}
